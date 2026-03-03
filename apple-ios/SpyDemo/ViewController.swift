import UIKit
import AVFoundation
import CoreLocation
import Contacts

// NOTE: Pour Apple (iOS), le système est très fermé ("Sandboxing").
// - Un keylogger système (qui voit tout l'iPhone) est IMPOSSIBLE sans Jailbreak.
// - Accès Caméra/Micro/GPS nécessite une autorisation explicite et affiche un point vert/orange en haut.

class ViewController: UIViewController, CLLocationManagerDelegate {

    let locationManager = CLLocationManager()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    func setupUI() {
        view.backgroundColor = .white
        
        let titleLabel = UILabel(frame: CGRect(x: 20, y: 100, width: 300, height: 40))
        titleLabel.text = "Audit Pédagogique iOS"
        titleLabel.font = .boldSystemFont(ofSize: 20)
        view.addSubview(titleLabel)

        let btnGPS = UIButton(frame: CGRect(x: 20, y: 200, width: 250, height: 50))
        btnGPS.setTitle("🌍 Test GPS", for: .normal)
        btnGPS.backgroundColor = .blue
        btnGPS.addTarget(self, action: #selector(testGPS), for: .touchUpInside)
        view.addSubview(btnGPS)
        
        let btnCam = UIButton(frame: CGRect(x: 20, y: 270, width: 250, height: 50))
        btnCam.setTitle("📸 Test Caméra", for: .normal)
        btnCam.backgroundColor = .red
        btnCam.addTarget(self, action: #selector(testCamera), for: .touchUpInside)
        view.addSubview(btnCam)
    }

    @objc func testGPS() {
        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        print("Demande accès GPS...")
    }

    @objc func testCamera() {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            if granted {
                print("L'utilisateur a donné accès à la caméra.")
            } else {
                print("Accès refusé.")
            }
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            print("Position iOS : \(location.coordinate.latitude), \(location.coordinate.longitude)")
        }
    }
}

/*
INFO IMPORTANTE POUR APPLE (Info.plist):
Il faut déclarer pourquoi vous voulez ces accès pour que l'app ne crashe pas :
- NSCameraUsageDescription
- NSMicrophoneUsageDescription
- NSLocationWhenInUseUsageDescription
- NSContactsUsageDescription
*/
