"""Usage: python3 install.py /path/to/Project4-backend. Makes backups of changed files."""
from pathlib import Path
import sys, shutil, datetime
source = Path(__file__).resolve().parent
root = Path(sys.argv[1]).resolve()
pom = root / 'pom.xml'
assert pom.is_file() and (root / 'src/main/java/com/toystorage/backend').is_dir(), 'Choose the backend root.'
stamp = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
def save(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        if path.read_text() == text: return
        shutil.copy2(path, path.with_name(path.name + '.backup-' + stamp))
    path.write_text(text)
text = pom.read_text()
if '<artifactId>spring-boot-starter-mail</artifactId>' not in text:
    text = text.replace('</dependencies>', '<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId></dependency>\n    </dependencies>', 1)
    save(pom, text)
for path in (source / 'src').rglob('*.java'):
    save(root / path.relative_to(source), path.read_text())
save(root / 'src/main/resources/application-password-reset.properties', (source / 'application-password-reset.properties').read_text())
print('Installed source and profile. Run schema.sql, configure SMTP, then enable password-reset profile. Existing SecurityConfig was preserved.')
