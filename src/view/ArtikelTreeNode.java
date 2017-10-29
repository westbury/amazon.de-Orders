package view;

import amazon.data.Artikel;

public class ArtikelTreeNode extends AmazonTreeNode {

	final Artikel artikel;
	
	public ArtikelTreeNode(Artikel artikel) {
		this.artikel = artikel;
	}

	@Override
	public String getDescription() {
		return artikel.name;
	}


}
