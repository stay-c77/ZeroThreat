import csv

URLSET_FILE = '/Users/sharonshalonmathew/Downloads/urlset.csv'
URLDATA_FILE = '/Users/sharonshalonmathew/Downloads/urldata.csv'
OUTPUT_FILE = '/Users/sharonshalonmathew/Documents/zerothreat-git/testing_data.csv'

print("="*60)
print("CREATING TESTING DATA FROM URL FILES")
print("="*60)

all_urls = []

# Extract from urlset.csv
print(f"\nExtracting 600 URLs from urlset.csv...")
try:
    with open(URLSET_FILE, 'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader):
            try:
                url = row.get('domain', '').strip()
                if url and url != '""' and len(url) > 5:
                    all_urls.append(url)
                    if len(all_urls) % 100 == 0:
                        print(f"  Extracted {len(all_urls)} URLs...")
                    if len(all_urls) >= 600:
                        break
            except:
                pass
except Exception as e:
    print(f"  Error: {e}")

urlset_count = len(all_urls)
print(f"✓ Extracted {urlset_count} URLs from urlset.csv")

# Extract from urldata.csv
print(f"\nExtracting 400 URLs from urldata.csv...")
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
                        print(f"  Extracted {current_urldata} URLs from urldata.csv...")
                    if current_urldata >= 400:
                        break
            except:
                pass
except Exception as e:
    print(f"  Error: {e}")

urldata_count = len(all_urls) - urlset_count
print(f"✓ Extracted {urldata_count} URLs from urldata.csv")

print(f"\n" + "="*60)
print("SUMMARY")
print("="*60)
print(f"URLSet:   {urlset_count}/600")
print(f"URLData:  {urldata_count}/400")
print(f"Total:    {len(all_urls)}/1000")

# Write to CSV
with open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['url'])
    for url in all_urls:
        writer.writerow([url])

print(f"\n✓ CSV file created: {OUTPUT_FILE}")
print(f"Total rows: {len(all_urls)}")
print("="*60)

