package jcrystal.utils.extractors;

import jcrystal.utils.langAndPlats.AbsCodeBlock;
import jcrystal.utils.langAndPlats.delegates.AbsCodeBlockDelegator;

public class AbsExtractor implements AbsCodeBlockDelegator{
	AbsCodeBlock delegate;
	@Override
	public AbsCodeBlock getDelegator() {
		return delegate;
	}
	public void setDelegate(AbsCodeBlock delegate) {
		this.delegate = delegate;
	}
}
