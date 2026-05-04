import csv
import socket
import re

# Configuration
URLSET_FILE = '/Users/sharonshalonmathew/Downloads/urlset.csv'
URLDATA_FILE = '/Users/sharonshalonmathew/Downloads/urldata.csv'
OUTPUT_FILE = '/Users/sharonshalonmathew/Documents/zerothreat-git/testing_data.csv'

# DNS settings
socket.setdefaulttimeout(2)
proto_re = re.compile(r'^https?://', re.IGNORECASE)
www_re = re.compile(r'^www\.', re.IGNORECASE)

def extract_domain(url: str) -> str:
    """Extract domain from URL"""
    d = url.strip()
    d = proto_re.sub('', d)
    d = www_re.sub('', d)
    d = d.split('/')[0]
    d = d.split('?')[0]
    return d.strip()

def check_dns(url: str) -> bool:
    """Check if URL passes DNS resolution"""
    try:
        domain = extract_domain(url)
        if not domain or len(domain) < 2:
            return False
        socket.gethostbyname(domain)
        return True
    except:
        return False

# Main execution
print("="*60)
print("CREATING TESTING DATA WITH DNS-PASSED URLS")
print("="*60)

all_urls = []

# Process urlset.csv
print(f"\nProcessing urlset.csv for 600 DNS-passed URLs...")
try:
    with open(URLSET_FILE, 'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        count = 0
        checked = 0
        for idx, row in enumerate(reader):
            try:
                url = row.get('domain', '').strip()
                if url and url != '""' and len(url) > 5:
                    if check_dns(url):
                        all_urls.append(url)
                        count += 1
                        if count % 50 == 0:
                            print(f"  Found {count} DNS-passed URLs from urlset.csv...")
                    checked += 1
                    if count >= 600:
                        break
                    if checked % 100 == 0:
                        print(f"  Checked {checked}, found {count}...")
            except:
                pass
except Exception as e:
    print(f"  Error: {e}")

print(f"✓ Collected {len(all_urls)} URLs from urlset.csv")

# Process urldata.csv
print(f"\nProcessing urldata.csv for 400 DNS-passed URLs...")
urldata_count = 0
try:
    with open(URLDATA_FILE,  'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        count = 0
        checked = 0
        for idx, row in enumerate(reader):
            try:
                url = row.get('url', '').strip()
                if url and url != '""' and len(url) > 5:
                    if check_dns(url):
                        all_urls.append(url)
                        urldata_count += 1
                        if urldata_count % 50 == 0:
                            print(f"  Found {urldata_count} DNS-passed URLs from urldata.csv...")
                    checked += 1
                    if urldata_count >= 400:
                        break
                    if checked % 100 == 0:
                        print(f"  Checked {checked}, found {urldata_count}...")
            except:
                pass
except Exception as e:
    print(f"  Error: {e}")

print(f"✓ Collected {urldata_count} URLs from urldata.csv")

print(f"\n" + "="*60)
print("SUMMARY")
print("="*60)
print(f"URLSet (DNS-passed):  {len(all_urls) - urldata_count}/600")
print(f"URLData (DNS-passed): {urldata_count}/400")
print(f"Total URLs:           {len(all_urls)}/1000")

# Write to CSV
with open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['url'])
    for url in all_urls:
        writer.writerow([url])

print(f"\n✓ CSV file created: {OUTPUT_FILE}")
print(f"  Total rows: {len(all_urls)}")
print("="*60)

