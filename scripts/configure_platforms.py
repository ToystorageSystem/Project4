from pathlib import Path
import plistlib
import xml.etree.ElementTree as ET
android = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', android)
p = Path('android/app/src/main/AndroidManifest.xml')
tree = ET.parse(p)
root = tree.getroot()
for name in ('android.permission.INTERNET', 'android.permission.CAMERA'):
    if not any(x.get('{%s}name' % android) == name for x in root.findall('uses-permission')):
        ET.SubElement(root, 'uses-permission', {'{%s}name' % android: name})
tree.write(p, encoding='utf-8', xml_declaration=True)
# Permit local HTTP only in the Android debug variant.
p = Path('android/app/src/debug/AndroidManifest.xml')
p.parent.mkdir(parents=True, exist_ok=True)
if p.exists():
    tree = ET.parse(p)
    root = tree.getroot()
else:
    root = ET.Element('manifest')
    tree = ET.ElementTree(root)
app = root.find('application')
if app is None:
    app = ET.SubElement(root, 'application')
app.set('{%s}usesCleartextTraffic' % android, 'true')
tree.write(p, encoding='utf-8', xml_declaration=True)
p = Path('ios/Runner/Info.plist')
with p.open('rb') as f:
    info = plistlib.load(f)
info['NSPhotoLibraryUsageDescription'] = 'Chọn ảnh bằng chứng khi kiểm nhận hoặc báo cáo hàng hỏng.'
info['NSCameraUsageDescription'] = 'Quét mã sản phẩm và vị trí để thực hiện công việc trong kho.'
info['NSLocalNetworkUsageDescription'] = 'Kết nối máy chủ ToyStorage trong mạng nội bộ.'
with p.open('wb') as f:
    plistlib.dump(info, f)
print('Đã cấu hình quyền camera và mạng. iOS dùng HTTPS cho API.')
# CocoaPods printing support, when the generated project has a Podfile.
podfile = Path('ios/Podfile')
if podfile.exists():
    text = podfile.read_text()
    if 'use_frameworks!' not in text:
        text = text.replace("target 'Runner' do", "target 'Runner' do\n  use_frameworks!")
        podfile.write_text(text)

# Keep deployment settings aligned with scanner/printer requirements on Apple silicon.
import re
project = Path('ios/Runner.xcodeproj/project.pbxproj')
if project.exists():
    text = project.read_text()
    def deployment(match):
        value = match.group(1)
        return 'IPHONEOS_DEPLOYMENT_TARGET = ' + (value if tuple(map(int, value.split('.'))) >= (15, 5) else '15.5') + ';'
    text = re.sub(r'IPHONEOS_DEPLOYMENT_TARGET = ([0-9.]+);', deployment, text)
    project.write_text(text)
if podfile.exists():
    text = podfile.read_text()
    text = re.sub(r"(?m)^\s*#?\s*platform\s*:ios\s*,\s*['\"][^'\"]+['\"]", "platform :ios, '15.5'", text)
    if not re.search(r'(?m)^platform :ios', text): text = "platform :ios, '15.5'\n" + text
    podfile.write_text(text)
for name, mode in [('Debug', 'debug'), ('Release', 'release')]:
    path = Path('ios/Flutter') / (name + '.xcconfig')
    if not path.exists(): continue
    text = path.read_text()
    text = re.sub(r'(?m)^EXCLUDED_ARCHS\[sdk=iphonesimulator\*\].*$', '', text)
    text = re.sub(r'(?m)^IPHONEOS_DEPLOYMENT_TARGET\s*=.*$', '', text)
    include = '#include? "Pods/Target Support Files/Pods-Runner/Pods-Runner.' + mode + '.xcconfig"'
    if 'Pods-Runner.' + mode + '.xcconfig' not in text: text = include + '\n' + text
    text = text.rstrip() + '\nEXCLUDED_ARCHS[sdk=iphonesimulator*] = i386\nIPHONEOS_DEPLOYMENT_TARGET = 15.5\n'
    path.write_text(text)
# Local development allows localhost HTTP without a global arbitrary-load exemption.
with p.open('rb') as f:
    info = plistlib.load(f)
info.setdefault('NSAppTransportSecurity', {})['NSAllowsLocalNetworking'] = True
with p.open('wb') as f:
    plistlib.dump(info, f)
