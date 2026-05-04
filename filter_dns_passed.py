import csv

# Read DNS check results to get indices of DNS-passed URLs
dns_passed_indices = set()
with open('dns_check_results_1000.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        if row['failure_reason'] == 'NONE':
            dns_passed_indices.add(int(row['index']))

print(f"Total DNS-passed URLs: {len(dns_passed_indices)}")
print(f"Total DNS-failed URLs: {1000 - len(dns_passed_indices)}")

# Read detector results and filter to keep only DNS-passed URLs
filtered_rows = []
with open('detector_results_1000.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        if int(row['index']) in dns_passed_indices:
            filtered_rows.append(row)

print(f"Filtered detector results: {len(filtered_rows)} rows")

# Write filtered results
with open('detector_results_dns_passed.csv', 'w', newline='', encoding='utf-8') as f:
    fieldnames = ['index', 'input_url', 'scanner_input', 'result', 'score', 'description', 'error']
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(filtered_rows)

print(f"\nNew file created: detector_results_dns_passed.csv")
print(f"Removed {1000 - len(filtered_rows)} URLs that failed DNS checks")

