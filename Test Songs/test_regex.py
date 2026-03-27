import re

test = "[00:00.000]Test"

patterns = [
    (r"\[", "just opening bracket"),
    (r"\[\d", "bracket + digit"),
    (r"\[\d\d:\d\d\.\d\d\d\]", "complete time tag"),
    (r"\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\]", "current pattern"),
]

for pattern, desc in patterns:
    regex = re.compile(pattern)
    match = regex.search(test)
    result = match.group(0) if match else "NO MATCH"
    print(f"{desc:40s} -> {result}")

# Now test with file content
print("\n=== Testing with file content ===")
with open("test.lrc", "r", encoding="utf-8") as f:
    content = f.read()

lines = content.splitlines()
print(f"Total lines: {len(lines)}")

for idx, line in enumerate(lines[:5]):
    match = regex.search(line)
    if match:
        print(f"Line {idx}: MATCH - {match.group(0)}")
    else:
        print(f"Line {idx}: NO MATCH - {line[:50]}")
