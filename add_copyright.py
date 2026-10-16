from pathlib import Path
import re

HEADER = """/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

"""

EXTENSIONS = {".kt", ".js", ".ts"}

EXCLUDE_DIRS = {
    ".git",
    ".idea",
    "node_modules",
    "build",
    "dist",
    "target",
    ".gradle"
}

updated = 0
skipped = 0


def remove_existing_header(content):
    """
    Entfernt einen vorhandenen Kommentarblock am Dateianfang,
    wenn darin Copyright oder License vorkommt.
    """

    pattern = r"^\s*/\*.*?\*/\s*"

    match = re.match(pattern, content, flags=re.DOTALL)

    if not match:
        return content

    block = match.group(0).lower()

    if "copyright" in block or "license" in block:
        return content[match.end():]

    return content


for file in Path(".").rglob("*"):

    if not file.is_file():
        continue

    if file.suffix not in EXTENSIONS:
        continue

    if any(part in EXCLUDE_DIRS for part in file.parts):
        continue

    text = file.read_text(encoding="utf-8")

    # Bereits korrekt?
    if text.startswith(HEADER):
        print(f"OK: {file}")
        skipped += 1
        continue

    lines = text.splitlines(True)

    insert_position = 0

    # Kotlin @file:-Annotationen erhalten
    while (
        insert_position < len(lines)
        and lines[insert_position].startswith("@file:")
    ):
        insert_position += 1

    prefix = "".join(lines[:insert_position])
    body = "".join(lines[insert_position:])

    body = remove_existing_header(body)

    new_content = prefix + HEADER + body

    file.write_text(new_content, encoding="utf-8")

    updated += 1
    print(f"UPDATED: {file}")

print()
print(f"Updated files : {updated}")
print(f"Unchanged files: {skipped}")
