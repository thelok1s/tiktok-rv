import sys
import xml.etree.ElementTree as ET

def patch_manifest(manifest_path):
    # Register the Android namespace to preserve it
    ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
    android_ns = '{http://schemas.android.com/apk/res/android}'
    
    tree = ET.parse(manifest_path)
    root = tree.getroot()
    
    # 1. Remove isSplitRequired from application tag
    app = root.find('application')
    if app is not None:
        is_split_required_attr = android_ns + 'isSplitRequired'
        if is_split_required_attr in app.attrib:
            del app.attrib[is_split_required_attr]
            print("Removed android:isSplitRequired from <application>")

        # 2. Remove meta-data related to splits and stamps
        meta_tags_to_remove = [
            'com.android.dynamic.apk.fused.modules',
            'com.android.stamp.source',
            'com.android.stamp.type',
            'com.android.vending.splits',
            'com.android.vending.derived.apk.id'
        ]
        
        removed_count = 0
        for meta in app.findall('meta-data'):
            name = meta.get(android_ns + 'name')
            if name in meta_tags_to_remove:
                app.remove(meta)
                removed_count += 1
                
        print(f"Removed {removed_count} split/stamp <meta-data> tags.")

    tree.write(manifest_path, encoding='utf-8', xml_declaration=True)
    print(f"Successfully patched {manifest_path}")

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python patch_manifest.py <AndroidManifest.xml>")
        sys.exit(1)
    
    patch_manifest(sys.argv[1])
