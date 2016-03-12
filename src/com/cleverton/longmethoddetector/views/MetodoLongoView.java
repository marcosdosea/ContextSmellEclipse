package com.cleverton.longmethoddetector.views;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import com.cleverton.longmethoddetector.model.InformacoesMetodoModel;
import com.cleverton.longmethoddetector.model.MetodoLongoProviderModel;
import com.cleverton.longmethoddetector.negocio.AnalisadorInformacoesMetodos;
import com.cleverton.longmethoddetector.negocio.CarregaSalvaArquivos;

public class MetodoLongoView extends ViewPart {
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.cleverton.longmethoddetector.views.MetodoLongoView";

	private TableViewer viewer;
	private MetodoLongoComparator comparator;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (MetodoLongoProviderModel.INSTANCE.metodosLongos == null) {
			new AnalisadorInformacoesMetodos().realizarNovaAnalise();
		}
	}
	
	public void createPartControl(Composite parent) {
		createViewer(parent);
		// set the sorter for the table
		comparator = new MetodoLongoComparator();
		viewer.setComparator(comparator);
		insertActionTable();
	}

	public String alterarDireotioAbsolutoPorWorkspace(String local) {
		String retorno = "";
		ArrayList<String> projetos = CarregaSalvaArquivos.carregarProjetos();
		for (String projeto : projetos) {
			local = local.replace("\\", "/");
			if (local.contains(projeto)) {
				String[] partesProjeto = projeto.split("/");
				String nomeProjeto = partesProjeto[partesProjeto.length-1];
				String novoLocal = local.replace(projeto, "");
				retorno = "/" + nomeProjeto + novoLocal;
				retorno = retorno.replace("/", "\\");
				return retorno;
			}
		}
		return retorno;
	}

	public void insertActionTable() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				InformacoesMetodoModel linha = (InformacoesMetodoModel) selection.getFirstElement();
				/*System.out.println();
				System.out.println("Diret�rio: "+linha.getDiretorioDaClasse()+"\n"
						+ "M�todo: " + linha.getNomeMetodo() + "\n"
						+ "Linha Inicial: " + linha.getLinhaInicial()+ "\n"
						+ "N�mero de linhas: " + linha.getNumeroLinhas());
				 */
				String localWorkspace = alterarDireotioAbsolutoPorWorkspace(linha.getDiretorioDaClasse());
				//System.out.println(novoWorkspace);
				IFile file = ResourcesPlugin.getWorkspace().getRoot()
						.getFile(new Path(localWorkspace));
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage();
				HashMap map = new HashMap();
				map.put(IMarker.LINE_NUMBER, linha.getLinhaInicial());
				map.put(IWorkbenchPage.CHANGE_EDITOR_AREA_SHOW, 
						"org.eclipse.ui.DefaultTextEditor");
				IMarker marker;
				try {
					marker = file.createMarker(IMarker.LINE_NUMBER);
					marker.setAttributes(map);
					IDE.openEditor(page, marker);
					marker.delete();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		createColumns(parent, viewer);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new ArrayContentProvider());
		// get the content for the viewer, setInput will call getElements in the
		// contentProvider
		viewer.setInput(MetodoLongoProviderModel.INSTANCE.metodosLongos);
		// make the selection available to other views
		getSite().setSelectionProvider(viewer);
		// define layout for the viewer
		viewer.getControl().setLayoutData(layoutViewer());
	}

	private GridData layoutViewer() {
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		return gridData;
	}

	public TableViewer getViewer() {
		return viewer;
	}

	// create the columns for the table
	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = { "Diret�rio", "M�todo", "Linha Inicial", "N� de Linhas" };
		int[] bounds = { 400, 200, 100, 100 };

		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				InformacoesMetodoModel p = (InformacoesMetodoModel) element;
				return p.getDiretorioDaClasse();
			}
		});

		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				InformacoesMetodoModel p = (InformacoesMetodoModel) element;
				return p.getNomeMetodo();
			}
		});

		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				InformacoesMetodoModel p = (InformacoesMetodoModel) element;
				return p.getLinhaInicial()+"";
			}
		});

		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				InformacoesMetodoModel p = (InformacoesMetodoModel) element;
				return p.getNumeroLinhas()+"";
			}
		});
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
				SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		column.addSelectionListener(getSelectionAdapter(column, colNumber));
		return viewerColumn;
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column,
			final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				viewer.getTable().setSortDirection(dir);
				viewer.getTable().setSortColumn(column);
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}

	//Used to update the viewer from outsite
	public void refresh(IWorkbenchPage page) {
		if (getViewer() == null) {
			try {
				page.hideView(page.findView(ID));
				page.showView(ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		} else {
			viewer.refresh();
		} 
	} 

	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
