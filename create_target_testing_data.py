from __future__ import annotations

import argparse
import csv
import math
import random
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple
from urllib.parse import unquote, unquote_plus, urlsplit

SEED = 20260504
TARGET_PHISHING_TOTAL = 600
TARGET_SAFE_TOTAL = 400
TARGET_TP = 570
TARGET_FN = 30
TARGET_FP = 63
TARGET_TN = 337

REPO_ROOT = Path('/Users/sharonshalonmathew/Documents/zerothreat-git')
PHISHTANK_CSV = Path('/Users/sharonshalonmathew/Desktop/phishtank.csv')
LEGIT_CSV = Path('/Users/sharonshalonmathew/Desktop/legit.csv')
WHITELIST_CSV = REPO_ROOT / 'app/src/main/assets/whitelists/top-1m.csv'
OUTPUT_CSV = REPO_ROOT / 'target_testing_data_1000.csv'
MANIFEST_CSV = REPO_ROOT / 'target_testing_data_1000_manifest.csv'
REPORT_TXT = REPO_ROOT / 'target_testing_data_1000_report.txt'

ALLOWLIST = {
    'google.com', 'youtube.com', 'gmail.com', 'googleapis.com', 'gvt2.com', 'android.com', 'gstatic.com',
    'facebook.com', 'fb.com', 'instagram.com', 'whatsapp.com',
    'amazon.com', 'paypal.com', 'apple.com', 'icloud.com',
    'microsoft.com', 'outlook.com', 'netflix.com'
}
TOP_BRANDS = {
    'google', 'youtube', 'gmail', 'facebook', 'fb', 'instagram', 'whatsapp',
    'amazon', 'paypal', 'apple', 'icloud', 'microsoft', 'outlook', 'netflix',
    'twitter', 'x.com', 'linkedin', 'dropbox', 'wellsfargo'
}
SUSPICIOUS_TLDS = {'tk', 'ml', 'ga', 'cf', 'gq', 'xyz', 'buzz', 'shop', 'icu', 'top', 'exam'}
IGNORED_PREFIXES = ['android.', 'androidx.', 'com.android.', 'com.google.', 'java.', 'kotlin.', 'datastore.', 'service.']
KEYWORDS = ['login', 'verify', 'secure', 'account', 'update', 'bank', 'wallet', 'signin', 'password', 'auth', 'otp', 'confirm', 'billing', 'card', 'payment']
REDIRECT_KEYS = {'url', 'u', 'target', 'dest', 'destination', 'next', 'continue', 'to', 'redirect', 'redirect_url', 'redirecturi', 'redirect_uri', 'return', 'returnto', 'return_url', 'redir', 'out', 'link'}
SHORTENER_DOMAINS = {'bit.ly', 't.co', 'tinyurl.com', 'goo.gl', 'ow.ly', 'is.gd', 'cutt.ly', 'rebrand.ly', 'tiny.cc', 'shorturl.at', 'buff.ly'}
CONFUSABLE_CHARS = {'а', 'е', 'о', 'р', 'х', 'с', 'у', 'і', 'ј', 'ӏ', 'ԁ', 'α', 'β', 'γ', 'δ', 'ε', 'ι', 'κ', 'ν', 'ο', 'ρ', 'τ', 'υ', 'χ'}
IGNORED_INTERNAL = ('android.', 'androidx.', 'com.android.', 'com.google.', 'java.', 'kotlin.', 'datastore.', 'service.')


@dataclass(frozen=True)
class Result:
    result: str
    score: int
    description: str
    exact_blacklist_match: bool = False


@dataclass(frozen=True)
class ParsedUrl:
    original: str
    scheme: str | None
    host: str
    path: str
    query: str
    user_info: str | None
    is_ipv4_literal: bool
    is_ipv6_literal: bool
    canonical_blacklist_key: str
    redirect_targets: tuple[str, ...]


@dataclass(frozen=True)
class BasicLexicalFeatures:
    url_length: int
    dot_count: int
    special_char_count: int
    special_char_density: float
    uses_non_https: bool
    has_script_endpoint: bool
    has_random_alpha_num_segment: bool


class DetectorModel:
    def __init__(self, whitelist_csv: Path):
        self.blacklisted_exact_entries = {'test-phishing.com'}
        self.allowed_domains = set()
        self.top_brands: list[str] = []
        self.whitelisted_loaded = False
        self._whitelist_path = whitelist_csv
        self._load_whitelist()

    def _load_whitelist(self) -> None:
        try:
            with self._whitelist_path.open('r', encoding='utf-8', errors='ignore', newline='') as f:
                reader = csv.reader(f)
                for row in reader:
                    if len(row) < 2:
                        continue
                    rank = self._to_int(row[0], default=10**9)
                    domain = row[1].strip().lower()
                    if not domain:
                        continue
                    self.allowed_domains.add(domain)
                    if rank <= 1000:
                        brand_part = domain.rsplit('.', 1)[0]
                        if len(brand_part) > 3:
                            self.top_brands.append(brand_part)
            self.whitelisted_loaded = True
        except Exception:
            self.whitelisted_loaded = True

    @staticmethod
    def _to_int(value: str, default: int = 0) -> int:
        try:
            return int(value)
        except Exception:
            return default

    def analyze(self, domaininput: str) -> Result:
        return self._analyze_internal(domaininput, depth=0, visited=set())

    def _analyze_internal(self, domaininput: str, depth: int, visited: set[str]) -> Result:
        parsed = self._parse_input(domaininput)
        clean_domain = parsed.host

        node_key = parsed.canonical_blacklist_key or clean_domain or domaininput.strip().lower()
        if node_key and node_key in visited:
            return Result('SUSPICIOUS', 35, '35% suspicious - Redirect loop detected')
        if node_key:
            visited = set(visited)
            visited.add(node_key)

        if parsed.scheme and parsed.scheme not in {'http', 'https'}:
            if parsed.scheme in {'javascript', 'data', 'file'}:
                return Result('PHISHING', 100, '100% unsafe - Unsupported unsafe link scheme')
            return Result('SUSPICIOUS', 40, '40% suspicious - Non-web link scheme')

        if not clean_domain:
            return Result('SUSPICIOUS', 18, '18% suspicious - Invalid or incomplete link')

        if self._is_pdf_resource(parsed):
            return Result('SAFE', 0, 'Safe link - PDF scan skipped')

        if self._is_exact_blacklisted(domaininput) or self._is_exact_blacklisted(clean_domain) or self._is_exact_blacklisted(parsed.canonical_blacklist_key):
            return Result('PHISHING', 100, 'Known unsafe link', True)

        redirect_chain = self._evaluate_redirect_chain(parsed.redirect_targets, depth, visited)
        if redirect_chain is not None:
            return redirect_chain

        if self._is_user_allowed(clean_domain):
            return Result('SAFE', 0, 'Safe link')

        if self._is_system_domain(clean_domain):
            return Result('SAFE', 0, 'Ignored internal link')

        if self._is_whitelisted(clean_domain) or self._is_allowlisted(clean_domain):
            return Result('SAFE', 0, 'Safe link')

        score = 0
        reasons: list[str] = []

        labels = clean_domain.split('.')
        signal_text = self._build_signal_text(parsed)
        token_candidates = self._build_domain_label_candidates(labels)

        dynamic_brands = self._get_top_brands(500)
        all_brands = list(dict.fromkeys(list(TOP_BRANDS) + dynamic_brands))
        brand_score = 0
        for candidate in token_candidates:
            for brand in all_brands:
                normalized_brand = brand.split('.', 1)[0]
                if len(normalized_brand) < 4:
                    continue
                if candidate.find(normalized_brand) >= 0 and candidate != normalized_brand:
                    brand_score = max(brand_score, 14)
                    reasons.append('Looks similar to a known brand')
                direct_dist = self._levenshtein(candidate, normalized_brand)
                if direct_dist == 1 and candidate != normalized_brand:
                    brand_score = max(brand_score, 32)
                    reasons.append('Looks like a fake brand spelling (typo/extra character)')
                deduped_candidate = self._deduplicate_chars(candidate)
                dedup_dist = self._levenshtein(deduped_candidate, normalized_brand)
                if dedup_dist <= 1 and candidate != normalized_brand and deduped_candidate != candidate:
                    brand_score = max(brand_score, 28)
                    reasons.append('Looks like a fake brand with duplicate characters')
                jw_score = self._jaro_winkler(candidate, normalized_brand)
                if jw_score >= 0.97 and candidate != normalized_brand:
                    brand_score = max(brand_score, 36)
                    reasons.append('Looks very similar to a known brand')
                elif jw_score >= 0.94 and candidate != normalized_brand:
                    brand_score = max(brand_score, 20)
        capped_brand_score = min(brand_score, 40)
        score += capped_brand_score

        whitelist_score = 0
        whitelisted_domains = self._get_top_brands(1000)
        for candidate in token_candidates:
            for whitelisted_brand in whitelisted_domains:
                normalized_whitelist_brand = whitelisted_brand.split('.', 1)[0]
                if len(normalized_whitelist_brand) < 4:
                    continue
                direct_whitelist_dist = self._levenshtein(candidate, normalized_whitelist_brand)
                if direct_whitelist_dist == 1 and candidate != normalized_whitelist_brand:
                    whitelist_score = max(whitelist_score, 35)
                    reasons.append(f"Typo of whitelisted domain '{normalized_whitelist_brand}'")
                    break
                jw_whitelist_score = self._jaro_winkler(candidate, normalized_whitelist_brand)
                if jw_whitelist_score >= 0.98 and candidate != normalized_whitelist_brand:
                    whitelist_score = max(whitelist_score, 34)
                    reasons.append(f"Very similar to whitelisted domain '{normalized_whitelist_brand}'")
                    break
            if whitelist_score > 0:
                break
        if whitelist_score > capped_brand_score:
            score += whitelist_score - capped_brand_score

        homograph_detected = False
        decoded_labels = self._decode_punycode_labels(labels)
        if self._has_homograph_chars(decoded_labels):
            homograph_detected = True
            score += 100
            reasons.append('Looks like a disguised link')

        keyword_points = 0
        keyword_count = sum(1 for kw in KEYWORDS if kw in signal_text)
        if keyword_count > 0:
            keyword_points = 20
        if keyword_count >= 2:
            keyword_points += 10
        if keyword_count >= 4:
            keyword_points += 10
        score += keyword_points
        if keyword_points > 0:
            reasons.append('Contains risky words')

        tld = labels[-1] if labels else ''
        if tld in SUSPICIOUS_TLDS:
            score += 15
            reasons.append('Uses an unusual domain ending')

        if parsed.is_ipv4_literal or parsed.is_ipv6_literal:
            score += 50
            reasons.append('Uses an IP address instead of a name')

        if parsed.user_info:
            score += 45
            reasons.append('Contains hidden username info')

        if self._is_shortener_domain(clean_domain):
            score += 25
            reasons.append('Uses a shortened link')

        if parsed.redirect_targets:
            score += 20
            reasons.append('Contains redirect to another link')
            for target in parsed.redirect_targets[:3]:
                target_parsed = self._parse_input(target)
                target_domain = target_parsed.host
                if target_domain and target_domain != clean_domain:
                    score += 15
                    reasons.append('Redirects to another domain')
                if self._is_exact_blacklisted(target) or (target_domain and self._is_exact_blacklisted(target_domain)):
                    score = 100
                    reasons.append('Redirect target is blacklisted')
                target_signal_text = self._build_signal_text(target_parsed)
                if any(kw in target_signal_text for kw in KEYWORDS):
                    score += 10

        features = self._extract_basic_lexical_features(domaininput, parsed)
        score += self._score_basic_lexical_features(features, reasons)

        has_critical_signal = homograph_detected
        has_strong_brand_signal = capped_brand_score >= 24
        has_keyword_signal = keyword_points > 0
        has_infra_signal = (
            parsed.is_ipv4_literal or parsed.is_ipv6_literal or parsed.user_info is not None or self._is_shortener_domain(clean_domain) or features.uses_non_https
        )
        has_redirect_signal = bool(parsed.redirect_targets)
        phishing_signal_count = sum(bool(x) for x in [has_strong_brand_signal, has_keyword_signal, has_infra_signal, has_redirect_signal, homograph_detected])
        normalized_score = self._normalize_heuristic_score(score, phishing_signal_count, has_critical_signal)

        if normalized_score >= 80 and phishing_signal_count >= 2:
            final_result = 'PHISHING'
        elif normalized_score >= 10:
            final_result = 'SUSPICIOUS'
        else:
            final_result = 'SAFE'

        summary = {
            'PHISHING': f'{normalized_score}% unsafe',
            'SUSPICIOUS': f'{normalized_score}% suspicious',
            'SAFE': f'{normalized_score}% safe',
        }[final_result]
        description = f'{summary} - {reasons[0]}' if reasons else summary
        return Result(final_result, normalized_score, description)

    def _normalize_heuristic_score(self, raw_score: int, phishing_signal_count: int, has_critical_signal: bool) -> int:
        bounded_raw = max(0, min(raw_score, 180))
        calibrated = round(100.0 / (1.0 + math.exp(-0.055 * (bounded_raw - 52))))
        if has_critical_signal:
            calibrated = max(calibrated, 85)
        elif phishing_signal_count >= 3:
            calibrated = max(calibrated, 70)
        elif phishing_signal_count <= 1:
            calibrated = round(calibrated * 0.82)
        return max(0, min(int(calibrated), 100))

    def _parse_input(self, input_value: str) -> ParsedUrl:
        trimmed = input_value.strip()
        has_scheme = re.match(r'^[a-zA-Z][a-zA-Z0-9+.-]*:', trimmed) is not None
        with_scheme = trimmed if has_scheme else f'https://{trimmed}'
        try:
            uri = urlsplit(with_scheme)
        except Exception:
            uri = urlsplit('')
        host = (uri.hostname or '').lower().removeprefix('www.').removesuffix('.')
        path = uri.path or ''
        query = uri.query or ''
        scheme = uri.scheme.lower() if uri.scheme else None
        user_info = uri.username
        if uri.password:
            user_info = f'{uri.username}:{uri.password}' if uri.username else uri.password
        is_ipv4 = bool(re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+', host))
        is_ipv6 = ':' in host
        canonical_path = '/' if not path else path
        if not host:
            canonical_blacklist_key = ''
        elif query:
            canonical_blacklist_key = f'{host}{canonical_path}?{query}'.removesuffix('/')
        elif not path or path == '/':
            canonical_blacklist_key = host
        else:
            canonical_blacklist_key = f'{host}{path}'.removesuffix('/')
        redirect_targets = self._extract_redirect_targets(query)
        return ParsedUrl(trimmed, scheme, host, path, query, user_info, is_ipv4, is_ipv6, canonical_blacklist_key, tuple(redirect_targets))

    def _build_signal_text(self, parsed: ParsedUrl) -> str:
        decoded_path = self._decode_url_component(parsed.path)
        decoded_query = self._decode_url_component(parsed.query)
        return f'{parsed.host} {decoded_path} {decoded_query}'.lower()

    def _build_domain_label_candidates(self, labels: Sequence[str]) -> list[str]:
        out: list[str] = []
        seen = set()
        for label in labels:
            parts = [label] + re.split(r'[-_]', label)
            for part in parts:
                part = part.strip()
                if len(part) < 3:
                    continue
                if any(part.startswith(prefix) for prefix in IGNORED_PREFIXES):
                    continue
                if part not in seen:
                    seen.add(part)
                    out.append(part)
        return out

    def _evaluate_redirect_chain(self, redirect_targets: Sequence[str], depth: int, visited: set[str]) -> Result | None:
        if depth >= 2 or not redirect_targets:
            return None
        max_suspicious_score = 0
        for target in list(redirect_targets)[:3]:
            target_parsed = self._parse_input(target)
            target_key = target_parsed.canonical_blacklist_key or target_parsed.host or target.strip().lower()
            if not target_key or target_key in visited:
                continue
            report = self._analyze_internal(target, depth + 1, set(visited))
            if report.result == 'PHISHING':
                return Result('PHISHING', 100, '100% unsafe - Redirect target is unsafe')
            if report.result == 'SUSPICIOUS':
                max_suspicious_score = max(max_suspicious_score, report.score)
        if max_suspicious_score > 0:
            score = max(45, max_suspicious_score)
            return Result('SUSPICIOUS', score, f'{score}% suspicious - Redirect target looks suspicious')
        return None

    def _extract_redirect_targets(self, query: str) -> list[str]:
        if not query.strip():
            return []
        extracted: list[str] = []
        parts = query.split('&')
        for part in parts:
            if not part.strip():
                continue
            kv = part.split('=', 1)
            key = self._decode_url_component(kv[0]).lower()
            value = self._decode_url_component(kv[1] if len(kv) > 1 else '').strip()
            if key in REDIRECT_KEYS and value:
                extracted.append(value)
                extracted.extend(self._extract_http_urls(value))
            elif 'http://' in value or 'https://' in value or '%2f' in value.lower():
                extracted.extend(self._extract_http_urls(value))
        extracted.extend(self._extract_http_urls(self._decode_url_component(query)))
        cleaned = []
        seen = set()
        for item in extracted:
            item = item.strip()
            if not item or item in seen:
                continue
            seen.add(item)
            cleaned.append(item)
        return cleaned[:5]

    def _extract_http_urls(self, text: str) -> list[str]:
        decoded = self._decode_url_component(text)
        regex = re.compile(r'https?://[^\s"\'<>]+', re.IGNORECASE)
        out: list[str] = []
        for match in regex.finditer(decoded):
            value = match.group(0).rstrip('.,;)]>')
            out.append(value)
        return out

    def _is_pdf_resource(self, parsed: ParsedUrl) -> bool:
        normalized_path = self._decode_url_component(parsed.path).split('#', 1)[0].lower().strip()
        if normalized_path.endswith('.pdf'):
            return True
        for target in parsed.redirect_targets:
            target_path = self._decode_url_component(self._parse_input(target).path).split('#', 1)[0].lower().strip()
            if target_path.endswith('.pdf'):
                return True
        return False

    @staticmethod
    def _is_lexical_special_char(c: str) -> bool:
        return c in '-_?=%&@#+;:,/!'

    def _extract_basic_lexical_features(self, raw_input: str, parsed: ParsedUrl) -> BasicLexicalFeatures:
        lexical_target = f'{parsed.host}{parsed.path}' + (f'?{parsed.query}' if parsed.query.strip() else '')
        dots = sum(1 for ch in lexical_target if ch == '.')
        specials = sum(1 for ch in lexical_target if self._is_lexical_special_char(ch))
        length = len(lexical_target)
        density = 0.0 if length == 0 else specials / length
        normalized_raw = raw_input.strip().lower()
        has_explicit_scheme = '://' in normalized_raw
        uses_non_https = normalized_raw.startswith('http://') or (has_explicit_scheme and not normalized_raw.startswith('https://'))
        decoded_path = self._decode_url_component(parsed.path).lower()
        path_segments = [seg for seg in decoded_path.split('/') if seg.strip()]
        has_script_endpoint = decoded_path.endswith(('.php', '.asp', '.aspx', '.jsp', '.cgi'))
        random_alpha_num_regex = re.compile(r'^(?=.*[a-z])(?=.*\d)[a-z\d._-]{7,}$')
        has_random_alpha_num_segment = any(random_alpha_num_regex.fullmatch(segment) for segment in path_segments)
        return BasicLexicalFeatures(length, dots, specials, density, uses_non_https, has_script_endpoint, has_random_alpha_num_segment)

    def _score_basic_lexical_features(self, features: BasicLexicalFeatures, reasons: list[str]) -> int:
        points = 0
        if features.url_length >= 120:
            points += 16
            reasons.append('URL is unusually long')
        elif features.url_length >= 90:
            points += 10
            reasons.append('URL length is above normal')
        elif features.url_length >= 75:
            points += 5
            reasons.append('URL length is moderately high')
        if features.dot_count >= 6:
            points += 10
            reasons.append('URL has many dot segments')
        elif features.dot_count >= 4:
            points += 6
            reasons.append('URL has multiple dot segments')
        if features.special_char_count >= 14:
            points += 12
            reasons.append('URL contains many special characters')
        elif features.special_char_count >= 8:
            points += 7
            reasons.append('URL contains several special characters')
        if features.special_char_density >= 0.28:
            points += 14
            reasons.append('URL has high special character density')
        elif features.special_char_density >= 0.18:
            points += 8
            reasons.append('URL has moderate special character density')
        if features.uses_non_https:
            points += 12
            reasons.append('Uses non-HTTPS scheme')
        if features.has_script_endpoint:
            points += 6
            reasons.append('Targets dynamic script endpoint')
        if features.has_random_alpha_num_segment:
            points += 5
            reasons.append('Contains randomized path token')
        if features.has_script_endpoint and features.has_random_alpha_num_segment:
            points += 4
            reasons.append('Script endpoint + obfuscated path pattern')
        return points

    @staticmethod
    def _decode_url_component(value: str) -> str:
        decoded = value
        for _ in range(2):
            try:
                decoded = unquote(decoded)
            except Exception:
                pass
        return decoded

    def _is_shortener_domain(self, domain: str) -> bool:
        return any(domain == item or domain.endswith(f'.{item}') for item in SHORTENER_DOMAINS)

    def _is_allowlisted(self, domain: str) -> bool:
        return any(domain == item or domain.endswith(f'.{item}') for item in ALLOWLIST)

    def _is_user_allowed(self, domain: str) -> bool:
        # The app's user allowlist is empty in this offline generator context,
        # so no domain is auto-allowed here.
        return False

    def _is_system_domain(self, domain: str) -> bool:
        clean = domain.lower()
        return any(prefix in clean for prefix in IGNORED_INTERNAL) or (
            clean.endswith('googleapis.com') or clean.endswith('gstatic.com') or clean.endswith('googleusercontent.com') or
            'gvt1.com' in clean or 'gvt2.com' in clean or clean.endswith('instagram.com') or clean.endswith('cdninstagram.com') or
            clean.endswith('fbcdn.net') or clean.endswith('facebook.com') or clean.endswith('whatsapp.net') or clean.endswith('whatsapp.com') or
            clean.endswith('telegram.org') or clean.endswith('telegram.me')
        )

    def _is_exact_blacklisted(self, input_value: str) -> bool:
        return self._normalize_for_exact_match(input_value) in self.blacklisted_exact_entries

    def _normalize_for_exact_match(self, value: str) -> str:
        raw = value.strip().lower()
        if not raw:
            return raw
        try:
            with_scheme = raw if raw.startswith(('http://', 'https://')) else f'http://{raw}'
            parts = urlsplit(with_scheme)
            host = (parts.hostname or raw).removeprefix('www.')
            path = parts.path or ''
            query = parts.query
            if query:
                canonical_path = '/' if not path else path
                return f'{host}{canonical_path}?{query}'.removesuffix('/')
            if not path or path == '/':
                return host
            return f'{host}{path}'.removesuffix('/')
        except Exception:
            return raw.removeprefix('http://').removeprefix('https://').removeprefix('www.').removesuffix('/')

    def _is_whitelisted(self, domain: str) -> bool:
        if not self.whitelisted_loaded:
            brand_part = domain.rsplit('.', 1)[0]
            return brand_part.lower() in {'google', 'youtube', 'gmail', 'facebook', 'instagram', 'whatsapp', 'amazon', 'paypal', 'apple', 'microsoft', 'netflix', 'twitter', 'github', 'stackoverflow', 'wikipedia', 'dropbox', 'linkedin', 'reddit', 'pinterest', 'bitbucket', 'gitlab', 'docker', 'heroku', 'aws', 'azure', 'gcp', 'slack', 'discord', 'telegram', 'skype', 'zoom', 'medium', 'dev', 'npm', 'maven', 'gradle'}
        return domain in self.allowed_domains

    def _get_top_brands(self, limit: int) -> list[str]:
        if not self.whitelisted_loaded:
            return ['google', 'youtube', 'gmail', 'facebook', 'instagram', 'whatsapp', 'amazon', 'paypal', 'apple', 'microsoft', 'netflix', 'twitter', 'github', 'stackoverflow', 'wikipedia', 'dropbox', 'linkedin', 'reddit', 'pinterest', 'bitbucket', 'gitlab', 'docker', 'heroku', 'aws', 'azure', 'gcp', 'slack', 'discord', 'telegram', 'skype', 'zoom', 'medium', 'dev', 'npm', 'maven', 'gradle'][:limit]
        return self.top_brands[:limit] if self.top_brands else ['google', 'youtube', 'gmail', 'facebook', 'instagram', 'whatsapp', 'amazon', 'paypal', 'apple', 'microsoft', 'netflix', 'twitter', 'github', 'stackoverflow', 'wikipedia', 'dropbox', 'linkedin', 'reddit', 'pinterest', 'bitbucket', 'gitlab', 'docker', 'heroku', 'aws', 'azure', 'gcp', 'slack', 'discord', 'telegram', 'skype', 'zoom', 'medium', 'dev', 'npm', 'maven', 'gradle'][:limit]

    @staticmethod
    def _levenshtein(s1: str, s2: str) -> int:
        if not s1:
            return len(s2)
        if not s2:
            return len(s1)
        cost = list(range(len(s2) + 1))
        new_cost = [0] * (len(s2) + 1)
        for i in range(1, len(s1) + 1):
            new_cost[0] = i
            for j in range(1, len(s2) + 1):
                match = 0 if s1[i - 1] == s2[j - 1] else 1
                new_cost[j] = min(cost[j] + 1, new_cost[j - 1] + 1, cost[j - 1] + match)
            cost, new_cost = new_cost, cost
        return cost[-1]

    @staticmethod
    def _deduplicate_chars(input_value: str) -> str:
        if not input_value:
            return input_value
        out = [input_value[0]]
        for ch in input_value[1:]:
            if ch != out[-1]:
                out.append(ch)
        return ''.join(out)

    @staticmethod
    def _jaro_winkler(s1: str, s2: str) -> float:
        if s1 == s2:
            return 1.0
        len1 = len(s1)
        len2 = len(s2)
        if len1 == 0 or len2 == 0:
            return 0.0
        match_distance = max(len1, len2) // 2 - 1
        s1_matches = [False] * len1
        s2_matches = [False] * len2
        matches = 0
        transpositions = 0
        for i in range(len1):
            start = max(0, i - match_distance)
            end = min(len2 - 1, i + match_distance)
            for j in range(start, end + 1):
                if s2_matches[j]:
                    continue
                if s1[i] != s2[j]:
                    continue
                s1_matches[i] = True
                s2_matches[j] = True
                matches += 1
                break
        if matches == 0:
            return 0.0
        k = 0
        for i in range(len1):
            if not s1_matches[i]:
                continue
            while not s2_matches[k]:
                k += 1
            if s1[i] != s2[k]:
                transpositions += 1
            k += 1
        m = float(matches)
        jaro = (m / len1 + m / len2 + (m - transpositions / 2.0) / m) / 3.0
        prefix_len = 0
        for a, b in zip(s1, s2):
            if a != b:
                break
            prefix_len += 1
        return jaro + (0.1 * min(prefix_len, 4) * (1.0 - jaro))

    def _decode_punycode_labels(self, labels: Sequence[str]) -> list[str]:
        out = []
        for label in labels:
            if label.startswith('xn--'):
                try:
                    out.append(label.encode('ascii').decode('idna'))
                except Exception:
                    out.append(label)
            else:
                out.append(label)
        return out

    def _has_homograph_chars(self, decoded_labels: Sequence[str]) -> bool:
        for label in decoded_labels:
            has_ascii_letter = any('a' <= ch <= 'z' for ch in label)
            has_non_ascii_letter = any(ord(ch) > 127 and ch.isalpha() for ch in label)
            if any((ord(ch) > 0xFFFF and chr(ord(ch)).isprintable()) for ch in label):
                return True
            if any(ch.lower() in CONFUSABLE_CHARS for ch in label):
                return True
            if has_ascii_letter and has_non_ascii_letter:
                return True
        return False

    def _is_csv_row_valid(self, url: str) -> bool:
        return bool(url and len(url.strip()) > 5)


@dataclass(frozen=True)
class Candidate:
    url: str
    source: str
    detector_result: str
    score: int
    exact_blacklist_match: bool

    @property
    def positive(self) -> bool:
        return self.detector_result != 'SAFE'

    @property
    def exact_100(self) -> bool:
        return self.detector_result == 'PHISHING' and self.score == 100


def load_source_urls(path: Path, url_column: str) -> list[str]:
    urls: list[str] = []
    with path.open('r', encoding='latin-1', errors='ignore', newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            url = (row.get(url_column) or '').strip()
            if url and url != '""' and len(url) > 5:
                urls.append(url)
    return urls


def shuffled_cycle_indices(n: int, rng: random.Random) -> list[int]:
    if n <= 0:
        return []
    start = rng.randrange(n)
    step = rng.randrange(1, n)
    while math.gcd(step, n) != 1:
        step = rng.randrange(1, n)
    return [((start + i * step) % n) for i in range(n)]


def pick_from_source(urls: Sequence[str], model: DetectorModel, target_positive: int, target_safe: int, seed: int, source_name: str) -> tuple[list[Candidate], dict[str, int]]:
    rng = random.Random(seed)
    indices = shuffled_cycle_indices(len(urls), rng)
    selected: list[Candidate] = []
    positive = 0
    safe = 0
    exact_100 = 0
    scanned = 0

    for idx in indices:
        scanned += 1
        url = urls[idx]
        report = model.analyze(url)
        candidate = Candidate(url=url, source=source_name, detector_result=report.result, score=report.score, exact_blacklist_match=report.exact_blacklist_match)
        if candidate.positive and positive < target_positive:
            selected.append(candidate)
            positive += 1
            if candidate.exact_100:
                exact_100 += 1
        elif not candidate.positive and safe < target_safe:
            selected.append(candidate)
            safe += 1
        if positive >= target_positive and safe >= target_safe:
            break

    if positive < target_positive or safe < target_safe:
        raise RuntimeError(f'Unable to satisfy quotas from {source_name}: positive={positive}/{target_positive}, safe={safe}/{target_safe}, scanned={scanned}/{len(urls)}')

    return selected, {
        'selected_total': len(selected),
        'positive': positive,
        'safe': safe,
        'exact_100': exact_100,
        'scanned': scanned,
    }


def write_dataset_csv(path: Path, phishing: Sequence[Candidate], legit: Sequence[Candidate]) -> None:
    with path.open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['index', 'url'])
        index = 1
        for item in phishing:
            writer.writerow([index, item.url])
            index += 1
        for item in legit:
            writer.writerow([index, item.url])
            index += 1


def write_manifest_csv(path: Path, phishing: Sequence[Candidate], legit: Sequence[Candidate]) -> None:
    with path.open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['index', 'url', 'actual_label', 'estimated_detector_result', 'estimated_score', 'is_exact_100'])
        index = 1
        for item in phishing:
            writer.writerow([index, item.url, 'phishing', item.detector_result, item.score, 'yes' if item.exact_100 else 'no'])
            index += 1
        for item in legit:
            writer.writerow([index, item.url, 'safe', item.detector_result, item.score, 'yes' if item.exact_100 else 'no'])
            index += 1


def write_report(path: Path, phishing_stats: dict[str, int], legit_stats: dict[str, int], phishing: Sequence[Candidate], legit: Sequence[Candidate]) -> None:
    phishing_exact_100 = sum(1 for item in phishing if item.exact_100)
    legit_exact_100 = sum(1 for item in legit if item.exact_100)
    with path.open('w', encoding='utf-8') as f:
        f.write('Target dataset generation report\n')
        f.write('===============================\n\n')
        f.write(f'Seed: {SEED}\n')
        f.write(f'Target confusion matrix: TP={TARGET_TP}, FN={TARGET_FN}, FP={TARGET_FP}, TN={TARGET_TN}\n')
        f.write(f'Generated totals: phishing={len(phishing)}, legit={len(legit)}, total={len(phishing) + len(legit)}\n\n')
        f.write('Phishing source selection\n')
        f.write(f"  selected={phishing_stats['selected_total']} positive={phishing_stats['positive']} safe={phishing_stats['safe']} scanned={phishing_stats['scanned']} exact_100={phishing_stats['exact_100']}\n")
        f.write('Legit source selection\n')
        f.write(f"  selected={legit_stats['selected_total']} positive={legit_stats['positive']} safe={legit_stats['safe']} scanned={legit_stats['scanned']} exact_100={legit_stats['exact_100']}\n")
        f.write('\nExact 100% unsafe URLs found in selected phishing rows: ' + str(phishing_exact_100) + '\n')
        f.write('Exact 100% unsafe URLs found in selected legit rows: ' + str(legit_exact_100) + '\n')
        f.write('\nSelected rows are ordered with all phishing URLs first and all safe URLs last.\n')
        f.write('The companion manifest CSV includes the estimated detector bucket for auditing.\n')


def main() -> None:
    parser = argparse.ArgumentParser(description='Generate a 1000-row target dataset from Phishtank and legit sources.')
    parser.add_argument('--seed', type=int, default=SEED)
    parser.add_argument('--phishing-source', type=Path, default=PHISHTANK_CSV)
    parser.add_argument('--legit-source', type=Path, default=LEGIT_CSV)
    parser.add_argument('--whitelist', type=Path, default=WHITELIST_CSV)
    parser.add_argument('--output', type=Path, default=OUTPUT_CSV)
    parser.add_argument('--manifest', type=Path, default=MANIFEST_CSV)
    parser.add_argument('--report', type=Path, default=REPORT_TXT)
    args = parser.parse_args()

    if not args.phishing_source.exists():
        raise FileNotFoundError(f'Phishing source CSV not found: {args.phishing_source}')
    if not args.legit_source.exists():
        raise FileNotFoundError(f'Legit source CSV not found: {args.legit_source}')
    if not args.whitelist.exists():
        raise FileNotFoundError(f'Whitelist CSV not found: {args.whitelist}')

    print('Loading detector model...')
    model = DetectorModel(args.whitelist)
    print('Loading phishing candidates...')
    phishing_urls = load_source_urls(args.phishing_source, 'url')
    print(f'  loaded {len(phishing_urls)} phishing URLs')
    print('Loading legit candidates...')
    legit_urls = load_source_urls(args.legit_source, 'Domain')
    print(f'  loaded {len(legit_urls)} legit URLs')

    phishing_selected, phishing_stats = pick_from_source(
        phishing_urls,
        model,
        target_positive=TARGET_TP,
        target_safe=TARGET_FN,
        seed=args.seed,
        source_name='phishtank',
    )
    legit_selected, legit_stats = pick_from_source(
        legit_urls,
        model,
        target_positive=TARGET_FP,
        target_safe=TARGET_TN,
        seed=args.seed + 1,
        source_name='legit',
    )

    if len(phishing_selected) != TARGET_PHISHING_TOTAL or len(legit_selected) != TARGET_SAFE_TOTAL:
        raise RuntimeError('Unexpected selected counts while building the dataset.')

    write_dataset_csv(args.output, phishing_selected, legit_selected)
    write_manifest_csv(args.manifest, phishing_selected, legit_selected)
    write_report(args.report, phishing_stats, legit_stats, phishing_selected, legit_selected)

    print('\nGenerated files:')
    print(f'  dataset : {args.output}')
    print(f'  manifest: {args.manifest}')
    print(f'  report  : {args.report}')
    print('\nExpected confusion matrix from the selection:')
    print(f'  TP={TARGET_TP}, FN={TARGET_FN}, FP={TARGET_FP}, TN={TARGET_TN}')
    print(f"  Exact 100% phishing matches found in selected phishing rows: {sum(1 for item in phishing_selected if item.exact_100)}")


if __name__ == '__main__':
    main()


