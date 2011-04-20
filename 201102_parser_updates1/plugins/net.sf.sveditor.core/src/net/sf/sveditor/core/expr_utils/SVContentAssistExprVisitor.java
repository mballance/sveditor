package net.sf.sveditor.core.expr_utils;

import java.util.List;
import java.util.Stack;

import net.sf.sveditor.core.db.ISVDBChildItem;
import net.sf.sveditor.core.db.ISVDBItemBase;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBClassDecl;
import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemBase;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBTask;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.expr.SVDBAssignExpr;
import net.sf.sveditor.core.db.expr.SVDBCastExpr;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.expr.SVDBFieldAccessExpr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;
import net.sf.sveditor.core.db.expr.SVDBParenExpr;
import net.sf.sveditor.core.db.expr.SVDBTFCallExpr;
import net.sf.sveditor.core.db.index.ISVDBIndexIterator;
import net.sf.sveditor.core.db.search.ISVDBFindNameMatcher;
import net.sf.sveditor.core.db.search.SVDBFindByName;
import net.sf.sveditor.core.db.search.SVDBFindByNameInClassHierarchy;
import net.sf.sveditor.core.db.search.SVDBFindByNameInScopes;
import net.sf.sveditor.core.db.search.SVDBFindNamedClass;
import net.sf.sveditor.core.db.search.SVDBFindSuperClass;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclItem;
import net.sf.sveditor.core.db.stmt.SVDBVarDeclStmt;

public class SVContentAssistExprVisitor {
	private ISVDBIndexIterator			fIndexIt;
	private ISVDBScopeItem				fScope;
	private SVDBClassDecl				fClassScope;
	private ISVDBFindNameMatcher		fNameMatcher;
	private Stack<ISVDBItemBase>		fResolveStack;
	
	private class SVAbortException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/*
		public SVAbortException() {
			super();
		}
		 */
		
		public SVAbortException(String msg) {
			super(msg);
		}
	}
	
	public SVContentAssistExprVisitor(
			ISVDBScopeItem			scope,
			ISVDBFindNameMatcher	name_matcher,
			ISVDBIndexIterator 		index_it) {
		fResolveStack = new Stack<ISVDBItemBase>();
		fScope = scope;
		fNameMatcher = name_matcher;
		fIndexIt = index_it;
		
		classifyScope();
	}
	
	private void classifyScope() {
		ISVDBChildItem parent = fScope;
		
		fClassScope = null;
		
		if (fScope == null) {
			return;
		}
		
		while (parent != null && parent.getType() != SVDBItemType.ClassDecl) {
			parent = parent.getParent();
		}
		
		if (parent != null && parent.getType() == SVDBItemType.ClassDecl) {
			fClassScope = (SVDBClassDecl)parent;
		} else {
			// See if this scope is an external task/function
			if (fScope.getType() == SVDBItemType.Function || 
					fScope.getType() == SVDBItemType.Task) {
				String name = ((SVDBTask)fScope).getName();
				int idx;
				if ((idx = name.indexOf("::")) != -1) {
					String class_name = name.substring(0, idx);
					System.out.println("class_name: " + class_name);
					SVDBFindNamedClass finder = new SVDBFindNamedClass(fIndexIt);
					List<SVDBClassDecl> result = finder.find(class_name);
					
					if (result.size() > 0) {
						fClassScope = result.get(0);
					}
				}
			}
		}
	}
	
	public ISVDBItemBase findItem(SVDBExpr expr) {
		fResolveStack.clear();
		
		try {
			visit(expr);
			if (fResolveStack.size() > 0) {
				return fResolveStack.pop();
			}
		} catch (SVAbortException e) {
			e.printStackTrace();
		}

		return null;
	}

	public ISVDBItemBase findTypeItem(SVDBExpr expr) {
		fResolveStack.clear();
		
		try {
			visit(expr);
			if (fResolveStack.size() > 0) {
				return findType(fResolveStack.pop());
			}
		} catch (SVAbortException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void visit(SVDBExpr expr) {
		switch (expr.getType()) {
				
			case CastExpr:
				cast_expr((SVDBCastExpr)expr);
				break;
				
			case FieldAccessExpr:
				field_access_expr((SVDBFieldAccessExpr)expr);
				break;
				
			case IdentifierExpr:
				identifier_expr((SVDBIdentifierExpr)expr);
				break;
				
			case TFCallExpr:
				tf_call((SVDBTFCallExpr)expr);
				break;

			case ParenExpr:
				visit(((SVDBParenExpr)expr).getExpr());
				break;

			case AssignExpr:
				assign_expr((SVDBAssignExpr)expr);
				break;

			case ClockingEventExpr:
			case ConcatenationExpr:
			case CondExpr:
			case CrossBinsSelectConditionExpr:
			case CtorExpr:
			case ArrayAccessExpr:
			case AssignmentPatternExpr:
			case AssignmentPatternRepeatExpr:
			case BinaryExpr:
			case RandomizeCallExpr:
			case RangeDollarBoundExpr:
			case RangeExpr:
			case UnaryExpr:
			case IncDecExpr:
			case InsideExpr:
			case LiteralExpr:
			case NamedArgExpr: // .ARG(value)
			case NullExpr:
				throw new SVAbortException("Unsupport expression element: " + expr.getType());
				
			default:
				throw new SVAbortException("Unhandled expression element: " + expr.getType());
		}
	}
	
	protected void cast_expr(SVDBCastExpr expr) {
		
	}

	protected void field_access_expr(SVDBFieldAccessExpr expr) {
		System.out.println("field_access_expr");
		visit(expr.getExpr());
		System.out.println(".");
		visit(expr.getLeaf());
	}
	
	private ISVDBItemBase findInScopeHierarchy(String name) {
		SVDBFindByNameInScopes finder = new SVDBFindByNameInScopes(fIndexIt);
		
		List<ISVDBItemBase> items = finder.find(fScope, name, true); 
		
		if (items.size() > 0) {
			return items.get(0);
		} else {
			return null;
		}
	}
	
	private ISVDBItemBase findInClassHierarchy(ISVDBChildItem root, String name) {
		SVDBFindByNameInClassHierarchy finder_h = 
			new SVDBFindByNameInClassHierarchy(fIndexIt, fNameMatcher);
		List<ISVDBItemBase> items = finder_h.find(root, name);
		
		if (items.size() > 0) {
			return items.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the type of the specified element.  
	 * @param item
	 * @return
	 */
	private ISVDBItemBase findType(ISVDBItemBase item) {
		SVDBTypeInfo type = null;
		if (item.getType() == SVDBItemType.VarDeclItem) {
			SVDBVarDeclStmt stmt = ((SVDBVarDeclItem)item).getParent();
			type = stmt.getTypeInfo();
		} else if (item.getType() == SVDBItemType.ClassDecl) {
			// NULL transform: item is already a type
			return item;
		}
		
		if (type != null) {
			if (type.getType() == SVDBItemType.TypeInfoUserDef) {
				SVDBFindByName finder_n = new SVDBFindByName(fIndexIt);

				List<ISVDBItemBase> item_l = finder_n.find(type.getName());
				
				if (item_l.size() > 0) {
					return item_l.get(0);
				}
			}
		}
		return null;
	}
	
	private ISVDBItemBase findRoot(String id) {
		ISVDBItemBase ret = null;
		List<ISVDBItemBase> r_l;
		
		if (id.equals("this") || id.equals("super")) {
			if (fClassScope != null) {
				if (id.equals("this")) {
					return fClassScope;
				} else {
					SVDBFindSuperClass finder = new SVDBFindSuperClass(fIndexIt);
					return finder.find(fClassScope);
				}
			}
		}
		
		// Check up the scopes in this class
		if ((ret = findInScopeHierarchy(id)) != null) {
			return ret;
		}
		
		// Check the super-class hierarchy
		if (fClassScope != null && (ret = findInClassHierarchy(fClassScope, id)) != null) {
			return ret;
		}

//		r_l = f
		
		return ret;
	}
	
	protected void identifier_expr(SVDBIdentifierExpr expr) {
		if (fResolveStack.size() == 0) {
			ISVDBItemBase item = findRoot(expr.getId());
			if (item == null) {
				throw new SVAbortException("Failed to find root \"" + expr.getId() + "\"");
			}
			fResolveStack.push(item);
		} else {
			System.out.println("Resolve : " + expr.getId() + " relative to " + fResolveStack.peek());
			ISVDBItemBase item = findType(fResolveStack.peek());
			
			if (item == null) {
				throw new SVAbortException("Failed to find type for \"" + fResolveStack.peek().getType() + "\"");
			}
			System.out.println("    item type: " + SVDBItem.getName(item));
			
			
			item = findInClassHierarchy((ISVDBChildItem)item, expr.getId());
			
			if (item == null) {
				throw new SVAbortException("Failed to find field \"" + expr.getId() + "\"");
			}
			
			fResolveStack.push(item);
		}
	}
	
	protected void tf_call(SVDBTFCallExpr expr) {
		if (fResolveStack.size() == 0) {
			// Resolve relative to the active context
		} else {
			// Resolve relative to the stack-top context
		}
	}

	protected void assign_expr(SVDBAssignExpr expr) {
		visit(expr.getLhs());
		visit(expr.getRhs());
	}

}
