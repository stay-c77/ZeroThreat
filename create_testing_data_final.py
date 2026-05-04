import csv

URLSET_FILE = '/Users/sharonshalonmathew/Downloads/urlset.csv'
URLDATA_FILE = '/Users/sharonshalonmathew/Downloads/urldata.csv'
DNS_RESULTS = '/Users/sharonshalonmathew/Documents/zerothreat-git/dns_check_results_1000.csv'
OUTPUT_FILE = '/Users/sharonshalonmathew/Documents/zerothreat-git/testing_data.csv'

print("="*60)
print("CREATING TESTING DATA - FINAL VERSION")
print("="*60)

all_urls = []

# First: Extract 600 URLs from urlset.csv (from the original 1000)
# These will be representative of the phishing/legitimate URLs
print(f"\n1. Extracting 600 URLs from urlset.csv...")
try:
    with open(URLSET_FILE, 'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader):
            try:
                url = row.get('domain', '').strip()
                if url and url != '""' and len(url) > 5:
                    all_urls.append(url)
                    if len(all_urls) % 100 == 0:
                        print(f"   Extracted {len(all_urls)} URLs...")
                    if len(all_urls) >= 600:
                        break
            except:
                pass
except Exception as e:
    print(f"   Error: {e}")

urlset_count = len(all_urls)
print(f"✓ Extracted {urlset_count} URLs from urlset.csv")

# Second: Extract 400 URLs from urldata.csv
# These are mostly legitimate websites that should have good DNS resolution
print(f"\n2. Extracting 400 URLs from urldata.csv...")
try:
    with open(URLDATA_FILE, 'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader):
            try:
                url = row.get('url', '').strip()
                if url and url != '""' and len(url) > 5:
                    all_urls.append(url)
                    current_urldata = len(all_urls) - urlset_count
                    if current_urldata % 50 == 0:
                        print(f"   Added {current_urldata} URLs from urldata.csv...")
                    if current_urldata >= 400:
                        break
            except:
                pass
except Exception as e:
    print(f"   Error: {e}")

urldata_count = len(all_urls) - urlset_count
print(f"✓ Extracted {urldata_count} URLs from urldata.csv")

print(f"\n" + "="*60)
print("SUMMARY")
print("="*60)
print(f"URLSet URLs:         {urlset_count}/600")
print(f"URLData URLs:        {urldata_count}/400")
print(f"Total URLs:          {len(all_urls)}/1000")
print(f"\nNOTE: URLdata.csv contains mostly legitimate domains")
print(f"that typically pass DNS validation (e.g., google.com,")
print(f"facebook.com, etc.). URLset.csv contains diverse URLs")
print(f"for testing both phishing and legitimate sites.")

# Write to CSV
with open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['url'])
    for url in all_urls:
        writer.writerow([url])

print(f"\n✓ CSV file created: testing_data.csv")
print(f"   Location: {OUTPUT_FILE}")
print(f"   Total rows: {len(all_urls)} data + 1 header = {len(all_urls) + 1} lines")
print("="*60)

