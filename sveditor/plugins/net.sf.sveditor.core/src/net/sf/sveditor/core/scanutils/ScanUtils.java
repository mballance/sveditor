package net.sf.sveditor.core.scanutils;

public class ScanUtils {
	
	public static String readHierarchicalId(IBIDITextScanner scanner, int ch) {
		StringBuilder str = new StringBuilder();
		String tmp;
	
		while ((tmp = scanner.readIdentifier(ch)) != null) {
			if (scanner.getScanFwd()) {
				str.append(tmp);
			} else {
				str.insert(0, tmp);
			}
			
			ch = scanner.get_ch();
			
			if (ch == '.') {
				if (scanner.getScanFwd()) {
					str.append((char)ch);
				} else {
					str.insert(0, (char)ch);
				}
				ch = scanner.get_ch();
			} else if (ch == ':') {
				ch = scanner.get_ch();
				if (ch == ':') {
					ch = scanner.get_ch();
					if (scanner.getScanFwd()) {
						str.append("::");
					} else {
						str.insert(0, "::");
					}
				} else {
					// back out and escape
					if (ch != -1) {
						scanner.unget_ch(ch);
					}
					scanner.unget_ch(':');
					break;
				}
			} else {
				scanner.unget_ch(ch);
				break;
			}
		}
		
		if (str.length() > 0) {
			return str.toString();
		} else {
			return null;
		}
	}

}
