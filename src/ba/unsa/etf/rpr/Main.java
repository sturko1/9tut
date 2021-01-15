package ba.unsa.etf.rpr;

import java.util.Scanner;

public class Main {
    public static String CONNECTION_URL = "jdbc:sqlite:baza.db";

    // Napomena: Zbog 'cirkularne' povezanosti izmedju tabela Drzava i Grad, odnosno postojanje foreign key glavni_grad - grad(id) u tabeli Drzava
    // i foreign key drzava - drzava(id) u tabeli Grad, dodavanje reda u tabelu Grad i reda u tabelu Drzava nekada moze stvoriti probleme
    // Na primjer, ukoliko se tek kreira grad Sarajevo a odmah i drzava BiH nije moguce dodati jedan red u tabelu a da se prethodno
    // nije dodao red u drugoj tabeli. Tacnije red u tabeli Drzava za drzavu BiH zahtijeva red u tabeli Grad za grad Sarajevo i obrnuto
    // Tada se javlja greska SQL Error FOREIGN KEY CONSTRAINT FAILED
    // Zbog navedenog, u ovom rjesenju su zanemarena foreign key ogranicenja (sto se moze vidjeti u baza.sql fajlu).
    // Kolege koje zele da ipak imaju foreign key ogranicenja morace skontati nacin kako zaobici ovu 'cirkluarnost'


    public static String ispisiGradove() {
        StringBuilder gradovi = new StringBuilder();
        GeografijaDAO dao = GeografijaDAO.getInstance();
        for (Grad grad : dao.gradovi())
            gradovi.append(grad.getNaziv()).append(" (").append(grad.getDrzava().getNaziv()).append(") - ").append(grad.getBrojStanovnika()).append("\n");

        return gradovi.toString();
    }

    public static void glavniGrad(String nazivDrzave) {
        GeografijaDAO dao = GeografijaDAO.getInstance();
        Grad glavniGrad = dao.glavniGrad(nazivDrzave);
        if (glavniGrad != null)
            System.out.println("Glavni grad države " + nazivDrzave + " je " + glavniGrad.getNaziv());
        else
            System.out.println("Nepostojeća država");
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println("1. Ispisi gradove\n2. Glavni grad drzave\n3. Izlaz");
            Scanner scanner = new Scanner(System.in);
            try {
                int ulaz = scanner.nextInt();
                if (ulaz == 1) {
                    System.out.println(ispisiGradove());
                } else if (ulaz == 2) {
                    System.out.println("Unesite naziv drzave: ");
                    scanner.nextLine();
                    String nazivDrzave = scanner.nextLine();
                    glavniGrad(nazivDrzave);
                } else if (ulaz == 3)
                    break;
                else
                    throw new IllegalArgumentException("Unos mora biti od 1 do 3");
            } catch (Exception e) {
                System.out.println("Neispravan unos. Molimo unesite ponovo: ");
            }
        }

        GeografijaDAO.removeInstance();
    }
}

