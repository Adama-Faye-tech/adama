#include <iostream>
#include <cmath>
using namespace std;
int main()
{
    double message; nprint; 
    int n ;
     count << "montant de prix :";endl;
     cin >> nprint;
}
count << "le prix est : " << endl; cin >>n;
count << "le taux est : " << endl; cin >> n;
message = mensualite=(nprint), n ,tia );
globale=globalte(mens,n,);
count << "le montant de mensualite est : " << message << endl;
count << "le montant de globale est : " << globale << endl;
return 0;
}
double mensualite(double nprint, int n, double tia)
double time = tia / 1200;
double mensualite = nprint * time / (1 - pow(1 + time, -n));
return mensualite;  
double globale(double mensualite, int n)
double globale = mensualite * 12 * n;








#include <iostream>
#include <cmath>
using namespace std;

double mensualite(double nprint, int n, double tia);
double globale(double mensualite, int n);

int main()
{
    double nprint, tia, message, total;
    int n;

    cout << "Montant du prix : ";
    cin >> nprint;

    cout << "Nombre d'annees : ";
    cin >> n;

    cout << "Taux d'interet annuel : ";
    cin >> tia;

    message = mensualite(nprint, n, tia);
    total = globale(message, n);

    cout << "La mensualite est : " << message << endl;
    cout << "Le montant global est : " << total << endl;

    return 0;
}

double mensualite(double nprint, int n, double tia)
{
    double time = tia / 1200;
    double m = nprint * time / (1 - pow(1 + time, -n * 12));
    return m;
}

double globale(double mensualite, int n)
{
    double g = mensualite * 12 * n;
    return g;
}