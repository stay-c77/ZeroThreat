import csv
import socket
import re
from collections import defaultdict

URLSET_FILE = '/Users/sharonshalonmathew/Downloads/urlset.csv'
URLDATA_FILE = '/Users/sharonshalonmathew/Downloads/urldata.csv'
OUTPUT_FILE = '/Users/sharonshalonmathew/Documents/zerothreat-git/testing_data.csv'

socket.setdefaulttimeout(1.0)
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

# DNS cache to avoid redundant checks
dns_cache = {}

def check_dns(url: str) -> bool:
    """Check if URL passes DNS resolution with caching"""
    try:
        domain = extract_domain(url)
        if not domain or len(domain) < 2:
            return False

        if domain in dns_cache:
            return dns_cache[domain]

        socket.gethostbyname(domain)
        dns_cache[domain] = True
        return True
    except:
        dns_cache[domain if domain else url] = False
        return False

print("="*60)
print("CREATING TESTING DATA WITH DNS-VALIDATION")
print("="*60)

all_urls = []

# Extract from urlset.csv with DNS check
print(f"\nExtracting 600 DNS-passed URLs from urlset.csv...")
try:
    with open(URLSET_FILE, 'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader):
            try:
                url = row.get('domain', '').strip()
                if url and url != '""' and len(url) > 5:
                    if check_dns(url):
                        all_urls.append(url)
                        if len(all_urls) % 50 == 0:
                            print(f"  DNS-validated {len(all_urls)} URLs...")
                        if len(all_urls) >= 600:
                            break
                if idx % 500 == 0:
                    print(f"  Checked {idx} URLs (found {len(all_urls)} valid)...")
            except:
                pass
except Exception as e:
    print(f"  Error: {e}")

urlset_count = len(all_urls)
print(f"✓ Extracted {urlset_count} DNS-passed URLs from urlset.csv")
print(f"  DNS cache size: {len(dns_cache)}")

# Extract from urldata.csv with DNS check
print(f"\nExtracting 400 DNS-passed URLs from urldata.csv...")
try:
    with open(URLDATA_FILE, 'r', encoding='latin-1', errors='ignore') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader):
            try:
                url = row.get('url', '').strip()
                if url and url != '""' and len(url) > 5:
                    if check_dns(url):
                        all_urls.append(url)
                        current_urldata = len(all_urls) - urlset_count
                        if current_urldata % 50 == 0:
                            print(f"  DNS-validated {current_urldata} URLs from urldata...")
                        if current_urldata >= 400:
                            break
                if idx % 500 == 0:
                    print(f"  Checked {idx} URLs (found {len(all_urls) - urlset_count} valid from urldata)...")
            except:
                pass
except Exception as e:
    print(f"  Error: {e}")

urldata_count = len(all_urls) - urlset_count
print(f"✓ Extracted {urldata_count} DNS-passed URLs from urldata.csv")
print(f"  Total DNS cache entries: {len(dns_cache)}")

print(f"\n" + "="*60)
print("SUMMARY")
print("="*60)
print(f"URLSet (DNS-passed):   {urlset_count}/600")
print(f"URLData (DNS-passed):  {urldata_count}/400")
print(f"Total URLs:            {len(all_urls)}/1000")
print(f"Unique domains cached: {len(dns_cache)}")

# Write to CSV
with open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['url'])
    for url in all_urls:
        writer.writerow([url])

print(f"\n✓ CSV file created: {OUTPUT_FILE}")
print(f"Total rows: {len(all_urls)}")
print("="*60)

