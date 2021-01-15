package ba.unsa.etf.rpr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

import static ba.unsa.etf.rpr.Main.CONNECTION_URL;

public class GeografijaDAO {

    private static GeografijaDAO geografijaDAO = null;
    private static Connection dbConnection;
    private PreparedStatement sortiraniGradoviUpit, dodajGradUpit, nadjiGradUpit, izmijeniGradUpit, glavniGradUpit;
    private PreparedStatement dodajDrzavuUpit, nadjiDrzavuUpit, obrisiDrzavuUpit;

    private GeografijaDAO() {
        try {
            dbConnection = DriverManager.getConnection(CONNECTION_URL);
            pripremiUpite();
        } catch (SQLException sqlException) {
            System.out.println(sqlException.getMessage());
            regenerisiBazu();
            try {
                pripremiUpite();
            } catch (SQLException sqlException1) {
                System.out.println(sqlException1.getMessage());
            }
        }
    }

    public static GeografijaDAO getInstance() {
        if (geografijaDAO == null)
            geografijaDAO = new GeografijaDAO();
        return geografijaDAO;
    }

    public static void removeInstance() {
        if (geografijaDAO != null) {
            try {
                dbConnection.close();
                geografijaDAO = null;
            } catch (SQLException sqlException) {
                System.out.println(sqlException.getMessage());
            }
        }
    }

    // Pomocne funkcije
    private void pripremiUpite() throws SQLException {
        sortiraniGradoviUpit = dbConnection.prepareStatement("SELECT * FROM grad ORDER BY broj_stanovnika DESC");
        dodajGradUpit = dbConnection.prepareStatement("INSERT INTO grad VALUES(?, ?, ?, ?)");
        nadjiGradUpit = dbConnection.prepareStatement("SELECT * FROM grad WHERE naziv=?");
        izmijeniGradUpit = dbConnection.prepareStatement("UPDATE grad SET broj_stanovnika=?, naziv=?, drzava=? WHERE id=?");
        glavniGradUpit = dbConnection.prepareStatement("SELECT glavni_grad FROM drzava WHERE naziv=?");

        dodajDrzavuUpit = dbConnection.prepareStatement("INSERT INTO drzava VALUES(?, ?, ?)");
        nadjiDrzavuUpit = dbConnection.prepareStatement("SELECT * FROM drzava WHERE naziv=?");
        obrisiDrzavuUpit = dbConnection.prepareStatement("DELETE FROM drzava WHERE naziv=?");

        Statement foreignKeyConstraint = dbConnection.createStatement();
        foreignKeyConstraint.execute("PRAGMA foreign_keys = ON");
    }

    private void regenerisiBazu() {
        System.out.println("Regenerisem...");
        Scanner ulaz = null;
        try {
            ulaz = new Scanner(new FileInputStream("baza.sql"));
            StringBuilder sqlUpit = new StringBuilder();
            while (ulaz.hasNext()) {
                sqlUpit.append(ulaz.nextLine());
                if (sqlUpit.length() > 1 && sqlUpit.charAt(sqlUpit.length() - 1) == ';') {
                    try {
                        Statement stmt = dbConnection.createStatement();
                        stmt.execute(sqlUpit.toString());
                        sqlUpit = new StringBuilder();
                    } catch (SQLException sqlException) {
                        System.out.println(sqlException.getMessage());
                    }
                }
            }
            ulaz.close();
        } catch (FileNotFoundException e) {
            System.out.println("Ne postoji SQL datoteka... nastavljam sa praznom bazom");
        }
    }

    private String dajNazivDrzaveId(int id) {
        try {
            Statement statement = dbConnection.createStatement();
            ResultSet drzavaSet = statement.executeQuery("SELECT naziv FROM drzava WHERE id = " + id);
            if (drzavaSet.next())
                return drzavaSet.getString(1);
        } catch (SQLException sqlException) {
            System.out.println(sqlException.getMessage());
        }

        return null;
    }

    private Grad nadjiGlavniGradDrzave(Drzava drzava) throws SQLException {
        String ggUpit = "SELECT g.id, g.naziv, g.broj_stanovnika, g.drzava FROM grad g, drzava d where g.id = d.glavni_grad AND d.id = " + drzava.getId();
        Statement ggUpitStmnt = dbConnection.createStatement();
        ResultSet resultSet = ggUpitStmnt.executeQuery(ggUpit);
        if (resultSet.next()) {
            return new Grad(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3), drzava);
        }
        return null;
    }

    // Interfejs
    public ArrayList<Grad> gradovi() {
        ArrayList<Grad> gradovi = new ArrayList<>();
        try {
            ResultSet resultSet = sortiraniGradoviUpit.executeQuery();
            while (resultSet.next()) {
                Drzava drzava = nadjiDrzavu(dajNazivDrzaveId(resultSet.getInt(4)));
                if (drzava != null) {
                    Grad grad = new Grad(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3), drzava);
                    gradovi.add(grad);
                }
            }
        } catch (SQLException sqlException) {
            System.out.println(sqlException.getMessage());
        }

        return gradovi;
    }

    public void dodajGrad(Grad grad) {
        try {
            dodajGradUpit.setInt(1, grad.getId());
            dodajGradUpit.setString(2, grad.getNaziv());
            dodajGradUpit.setInt(3, grad.getBrojStanovnika());
            dodajGradUpit.setInt(4, grad.getDrzava().getId());

            dodajGradUpit.executeUpdate();
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom dodavanja grada\nIzuzetak: " + sqlException.getMessage());
        }
    }

    public Grad nadjiGrad(String nazivGrada) {
        try {
            nadjiGradUpit.setString(1, nazivGrada);
            ResultSet resultSet = nadjiGradUpit.executeQuery();
            if (resultSet.next()) {
                Grad grad = new Grad();
                grad.setId(resultSet.getInt(1));
                grad.setNaziv(resultSet.getString(2));
                grad.setBrojStanovnika(resultSet.getInt(3));
                grad.setDrzava(nadjiDrzavu(dajNazivDrzaveId(resultSet.getInt(4))));
                return grad;
            }
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom pretrage grada\nIzuzetak: " + sqlException.getMessage());
        }
        return null;
    }

    public void izmijeniGrad(Grad grad) {
        try {
            izmijeniGradUpit.setInt(1, grad.getBrojStanovnika());
            izmijeniGradUpit.setString(2, grad.getNaziv());
            izmijeniGradUpit.setInt(3, grad.getDrzava().getId());
            izmijeniGradUpit.setInt(4, grad.getId());

            izmijeniGradUpit.executeUpdate();
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom izmjene grada\nIzuzetak: " + sqlException.getMessage());
        }
    }

    public Grad glavniGrad(String nazivDrzave) {
        try {
            glavniGradUpit.setString(1, nazivDrzave);
            ResultSet resultSet = glavniGradUpit.executeQuery();

            if (resultSet.next()) {
                int gradId = resultSet.getInt(1);
                Statement dajNazivZaId = dbConnection.createStatement();
                ResultSet rsGrad = dajNazivZaId.executeQuery("SELECT naziv FROM grad WHERE id = " + gradId);
                if (rsGrad.next())
                    return nadjiGrad(rsGrad.getString(1));
            }
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom dohvatanja glavnog grada\nIzuzetak: " + sqlException.getMessage());
            return null;
        }

        return null;
    }

    public void dodajDrzavu(Drzava drzava) {
        try {
            dodajDrzavuUpit.setInt(1, drzava.getId());
            dodajDrzavuUpit.setString(2, drzava.getNaziv());
            dodajDrzavuUpit.setInt(3, drzava.getGlavniGrad().getId());

            dodajDrzavuUpit.executeUpdate();
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom dodavanja drzave\nIzuzetak: " + sqlException.getMessage());
        }
    }

    public Drzava nadjiDrzavu(String nazivDrzave) {
        try {
            nadjiDrzavuUpit.setString(1, nazivDrzave);
            ResultSet rsDrzava = nadjiDrzavuUpit.executeQuery();
            if (rsDrzava.next()) {
                Drzava drzava = new Drzava();
                drzava.setId(rsDrzava.getInt(1));
                drzava.setNaziv(rsDrzava.getString(2));
                Grad glavniGrad = nadjiGlavniGradDrzave(drzava);
                if (glavniGrad != null) {
                    drzava.setGlavniGrad(glavniGrad);
                    return drzava;
                }
            }
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom pretrage drzave.\nIzuzetak: " + sqlException.getMessage());
        }

        return null;
    }

    public void obrisiDrzavu(String nazivDrzave) {
        try {
            obrisiDrzavuUpit.setString(1, nazivDrzave);
            obrisiDrzavuUpit.executeUpdate();
        } catch (SQLException sqlException) {
            System.out.println("Greska prilikom brisanja drzave\nIzuzetak: " + sqlException.getMessage());
        }
    }
}
