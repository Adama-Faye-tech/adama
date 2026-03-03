#!/system/bin/sh
# Script shell pour enregistrer les événements d'entrée (root nécessaire)

# Trouver le périphérique d'entrée du clavier
KEYBOARD_DEVICE=$(getevent -p | grep -B 1 "keyboard" | grep "add device" | awk '{print $4}' | tr -d ':')

if [ -n "$KEYBOARD_DEVICE" ]; then
    echo "Enregistrement des événements du clavier..."
    echo "Appuyez sur Ctrl+C pour arrêter"
    
    # Enregistrer dans un fichier
    getevent -lt "$KEYBOARD_DEVICE" > /sdcard/keylog_root.txt
else
    echo "Périphérique clavier non trouvé"
fi
