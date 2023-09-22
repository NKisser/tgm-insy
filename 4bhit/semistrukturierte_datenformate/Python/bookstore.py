import xml.etree.ElementTree as ET

def vizualize(path):
    tree = ET.parse(path)
    root = tree.getroot()
    categories = {}
    
    for book in root:
        category = book.attrib['category']
        if category not in categories:
            categories[category] = []
        categories[category].append(book)

    for category, books in categories.items():
        print(category + ':')
        for book in books:
            print('\t' + book[0].text)
            for child in book:
                print('\t\t' + child.tag + ' : ' + child.text)
            print()


vizualize("book_store.xml")