import struct
import re

filepath = r'D:\My things\Learn\高一\Music Player\Revive\Test Songs\Caslow、Sierra Annie、Jack The Underdog、Ventaria - Loveless Generation (Explicit).flac'

# LRC parser regex (same as Kotlin)
time_tag_regex = re.compile(r'\[(\d{1,2}):(\d{1,2})[.:](\d{1,3})\]')

def parse_time_ms(minutes: str, seconds: str, milliseconds: str) -> int:
    """Simulate Kotlin parseTimeMs function"""
    mins = min(int(minutes), 59)
    secs = min(int(seconds), 59)
    
    # Handle milliseconds (could be 1, 2, or 3 digits)
    if len(milliseconds) == 1:
        ms = int(milliseconds) * 100
    elif len(milliseconds) == 2:
        ms = int(milliseconds) * 10
    elif len(milliseconds) == 3:
        ms = int(milliseconds)
    else:
        ms = 0
    
    ms = min(ms, 999)
    return mins * 60 * 1000 + secs * 1000 + ms

# Extract lyrics from FLAC
with open(filepath, 'rb') as f:
    magic = f.read(4)
    
    if magic == b'fLaC':
        while True:
            header_bytes = f.read(4)
            if len(header_bytes) < 4:
                break
                
            header = struct.unpack('>I', header_bytes)[0]
            is_last = (header >> 24) & 0x80 != 0
            block_type = (header >> 24) & 0x7F
            block_size = header & 0xFFFFFF
            
            data = f.read(block_size)
            
            if block_type == 4:  # Vorbis Comment
                vendor_len = struct.unpack('<I', data[0:4])[0]
                offset = 4 + vendor_len
                num_comments = struct.unpack('<I', data[offset:offset+4])[0]
                offset += 4
                
                for i in range(num_comments):
                    comment_len = struct.unpack('<I', data[offset:offset+4])[0]
                    offset += 4
                    
                    raw_comment_bytes = data[offset:offset+comment_len]
                    offset += comment_len
                    
                    eq_pos = raw_comment_bytes.find(b'=')
                    if eq_pos > 0:
                        key = raw_comment_bytes[:eq_pos].decode('utf-8', errors='replace').upper()
                        
                        if key == 'LYRICS':
                            value_bytes = raw_comment_bytes[eq_pos+1:]
                            
                            print('=== EXTRACTED LYRICS FROM FLAC ===')
                            print(f'Key: {key}')
                            print(f'Value bytes: {len(value_bytes)} bytes')
                            print()
                            
                            # Decode and split lines (using splitlines like Kotlin's .lines())
                            value = value_bytes.decode('utf-8', errors='replace')
                            lines = value.splitlines()
                            
                            print(f'Total lines after split: {len(lines)}')
                            print()
                            
                            # Parse each line with regex
                            print('=== PARSING LINES ===')
                            parsed_count = 0
                            error_count = 0
                            
                            for idx, line in enumerate(lines[:15]):  # First 15 lines
                                line = line.strip()
                                if not line:
                                    continue
                                
                                match = time_tag_regex.search(line)
                                if match:
                                    minutes, seconds, ms = match.groups()
                                    time_ms = parse_time_ms(minutes, seconds, ms)
                                    
                                    # Extract text after last time tag
                                    all_matches = list(time_tag_regex.finditer(line))
                                    if all_matches:
                                        last_match = all_matches[-1]
                                        text = line[last_match.end():].strip()
                                    else:
                                        text = line
                                    
                                    print(f'Line {idx}: [{minutes}:{seconds}.{ms}] -> {time_ms} ms')
                                    print(f'  Text: {text[:50]}')
                                    parsed_count += 1
                                else:
                                    print(f'Line {idx}: NO MATCH - {line[:50]}')
                                    error_count += 1
                            
                            print()
                            print(f'Summary: {parsed_count} parsed, {error_count} failed')
                            
                            # Check what the "7-digit number" issue might be
                            print()
                            print('=== CHECKING FOR 7-DIGIT NUMBER ISSUE ===')
                            first_line = lines[0].strip()
                            match = time_tag_regex.search(first_line)
                            if match:
                                minutes, seconds, ms = match.groups()
                                print(f'First time tag: [{minutes}:{seconds}.{ms}]')
                                
                                # Simulate buggy parsing (old logic)
                                ms_val = int(ms)  # Just parse the number directly
                                buggy_total = int(minutes) * 60 * 1000 + int(seconds) * 1000 + ms_val
                                print(f'Buggy parsing (direct ms): {buggy_total} (this would show as large number)')
                                
                                # Correct parsing
                                correct_total = parse_time_ms(minutes, seconds, ms)
                                print(f'Correct parsing: {correct_total}')
                            
                            break
                
            if is_last:
                break

print()
print('=== TESTING WITH DIRECT FILE READ ===')
# Also test reading the test.lrc file we created
try:
    with open('test.lrc', 'r', encoding='utf-8') as f:
        content = f.read()
    
    lines = content.splitlines()
    print(f'test.lrc has {len(lines)} lines')
    
    for idx, line in enumerate(lines[:5]):
        match = time_tag_regex.search(line)
        if match:
            minutes, seconds, ms = match.groups()
            time_ms = parse_time_ms(minutes, seconds, ms)
            print(f'Line {idx}: {time_ms} ms - {line[12:50]}')
except Exception as e:
    print(f'Error: {e}')
