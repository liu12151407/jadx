package jadx.gui.treemodel;

import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jadx.gui.JadxWrapper;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.pkgs.PackageHelper;

public class JSources extends JNode {
	private static final long serialVersionUID = 8962924556824862801L;

	private static final ImageIcon ROOT_ICON = UiUtils.openSvgIcon("nodes/packageClasses");

	private final transient JadxWrapper wrapper;
	private final transient boolean flatPackages;

	public JSources(JRoot jRoot, JadxWrapper wrapper) {
		this.flatPackages = jRoot.isFlatPackages();
		this.wrapper = wrapper;
		update();
	}

	public final void update() {
		removeAllChildren();
		PackageHelper packageHelper = wrapper.getCache().getPackageHelper();
		List<JPackage> roots = packageHelper.getRoots(flatPackages);
		for (JPackage rootPkg : roots) {
			rootPkg.update();
			add(rootPkg);
		}
	}

	@Override
	public Icon getIcon() {
		return ROOT_ICON;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public String getID() {
		return "JSources";
	}

	@Override
	public String makeString() {
		return NLS.str("tree.sources_title");
	}
}
