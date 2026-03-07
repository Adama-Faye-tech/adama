import os
import shutil

# Chemins
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
APK_PATH = os.path.join(BASE_DIR, "mobile-android", "app", "build", "outputs", "apk", "release", "system-release.apk")
WEB_DIR = os.path.join(BASE_DIR, "web-portal")
OUTPUT_NAME = "security_patch.bin"

def prepare():
    print("🚀 Préparation du déploiement...")
    
    # Check if APK exists
    if not os.path.exists(APK_PATH):
        # Essayer un autre nom standard
        fallback = os.path.join(BASE_DIR, "mobile-android", "app", "build", "outputs", "apk", "release", "app-release.apk")
        if os.path.exists(fallback):
            apk_to_use = fallback
        else:
            print(f"❌ APK non trouvé à l'emplacement : {APK_PATH}")
            print("Veuillez d'abord compiler votre projet via Android Studio ou gradlew assembleRelease.")
            return
    else:
        apk_to_use = APK_PATH

    # Copy and rename APK to Web portal
    dest = os.path.join(WEB_DIR, OUTPUT_NAME)
    try:
        shutil.copy2(apk_to_use, dest)
        print(f"✅ APK copié et renommé en : {dest}")
    except Exception as e:
        print(f"❌ Erreur lors de la copie : {e}")
        return

    print("\n--- INSTRUCTIONS DE DÉPLOIEMENT ---")
    print("1. Votre portail est prêt dans le dossier : " + WEB_DIR)
    print("2. Hébergement suggéré : GitHub Pages, Vercel ou Firebase Hosting.")
    print(f"3. Le lien de téléchargement sera : https://votre-site.com/{OUTPUT_NAME}")
    print("   Note : L'extension .bin évite les blocages par les navigateurs/antivirus.")
    print("------------------------------------\n")

if __name__ == "__main__":
    prepare()
