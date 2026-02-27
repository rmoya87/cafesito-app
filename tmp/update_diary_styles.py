import os

file_path = r'c:\Users\ramon.demoya\.gemini\antigravity\scratch\cafesito-app-android\webApp\src\styles\features.css'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace Analytics Card Styles
old_card = """.diary-analytics-card {
  border-radius: var(--radius-card);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #000000;
  padding: 16px 14px 12px;
  min-width: 0;
  min-height: 430px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}"""

# We look for a more robust match if the exact string fails
if old_card not in content:
    # Try with different line endings or partial matches
    content = content.replace('border-radius: var(--radius-card);', 'border-radius: 28px;')
    content = content.replace('background: #000000;', 'background: #1a1512;')
    content = content.replace('padding: 16px 14px 12px;', 'padding: 24px 20px 18px; box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45);')
else:
    new_card = """.diary-analytics-card {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #1a1512;
  padding: 24px 20px 18px;
  min-width: 0;
  min-height: 440px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45);
}"""
    content = content.replace(old_card, new_card)

# Replace Meta Label Styles
content = content.replace('.diary-entry-meta-label {', '.diary-entry-meta-label {\n  color: var(--caramel-soft);\n  font-weight: 500;\n  text-transform: uppercase;\n  letter-spacing: 0.02em;')

# Improve Metric Box Icon
content = content.replace('.diary-metric-box .ui-icon {', '.diary-metric-box .ui-icon {\n  color: var(--caramel-soft);')

# Improve Brand Color
content = content.replace('.diary-entry-brand {', '.diary-entry-brand {\n  color: var(--caramel-soft);')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Replacement complete.")
