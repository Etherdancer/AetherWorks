import os
import re

files_to_update = [
    "app/src/main/res/values/strings.xml",
    "settings.gradle.kts",
    "app/src/main/java/org/example/aetherworks/ui/auth/LockScreen.kt",
    "app/src/main/java/org/example/aetherworks/ui/auth/OnboardingScreen.kt",
    "app/src/main/java/org/example/aetherworks/ui/main/MainScreen.kt",
    "app/src/main/java/org/example/aetherworks/ui/about/AboutScreen.kt",
    "AGENTS.md",
    "README.md",
    "PRIVACY.md",
    "CONTRIBUTING.md"
]

for filepath in files_to_update:
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Replace 'AetherWorks' with 'Clear Space' but avoid classes like AetherWorksApp
        # by checking that the next character is not an alphabetical character.
        new_content = re.sub(r'AetherWorks(?![a-zA-Z])', 'Clear Space', content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
    else:
        print(f"Skipped {filepath} - not found")
