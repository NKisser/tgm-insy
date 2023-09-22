package asuender.insy.xml;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Andreas Suender
 * @version 10-10-2022
 */
public class BookStore {
    private static final String PATH = "./book_store.xml";

    public static void main(String[] args) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new File(PATH));

        Element root = doc.getRootElement();
        List<Element> books = root.getChildren("book");

        for (Element book : books) {
            System.out.printf("Buch : [Kategorie : %s]%n", book.getAttributeValue("category"));
            System.out.printf("Titel : %s%n", book.getChildText("title"));
            System.out.printf("Autor : %s%n", book.getChildText("author"));
            System.out.printf("Erscheinungsjahr : %d%n", Integer.parseInt(book.getChildText("year")));
            System.out.printf("Preis : %.2f%n%n", Double.parseDouble(book.getChildText("price")));
        }
    }
}
