package it.polito.tdp.extflightdelays.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	
	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	private ExtFlightDelaysDAO dao;
	private Map<Integer, Airport> identityMap;
	private Map<Airport, Airport> visita; //ci salviamo l'albero di visita (i padri)
	
	public Model() {
		
		dao=new ExtFlightDelaysDAO();
		identityMap= new HashMap<Integer, Airport>();
		dao.loadAllAirports(identityMap); //la creo direttamente nel dao
		
	}
	
	public void creaGrafo(int x){
		
		this.grafo= new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		//Graphs.addAllVertices(this.grafo, identityMap.values()); devo filtrare i vertici
		//con una query
		
		Graphs.addAllVertices(this.grafo, dao.getVertici(x, identityMap));
		
		//aggiungo gli archi, creo classe rotta
		
		for(Rotta r: dao.getRotte(identityMap)) {
			
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
				//rotta di interesse, presenti nel grafo
				
				//c'è gia un arco?
				DefaultWeightedEdge e=this.grafo.getEdge(r.getA1(), r.getA2());
				
				if(e==null) { //no
					Graphs.addEdgeWithVertices(this.grafo, r.getA1(), r.getA2(), r.getN());
					
				}
				else {
					double pesoVecchio=this.grafo.getEdgeWeight(e);
					double pesoNuovo=pesoVecchio+ r.getN();
					this.grafo.setEdgeWeight(e, pesoNuovo);
				}
				
				
			}
			
		}
		
		System.out.println("Grafo creato");
		System.out.println("Num vertici: "+grafo.vertexSet().size());
		System.out.println("Num archi: "+grafo.edgeSet().size());
		
		
		
		
	}

	public Set<Airport> getVertici() {
		
		return grafo.vertexSet();
	}
	
	public int getNVertici() {
		if(grafo != null)
			return grafo.vertexSet().size();
		
		return 0;
	}
	
	public int getNArchi() {
		if(grafo != null)
			return grafo.edgeSet().size();
		
		return 0;
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2){
		
		List<Airport> percorso= new LinkedList<Airport>();
		
		BreadthFirstIterator<Airport, DefaultWeightedEdge> it =new BreadthFirstIterator<>(grafo, a1);
		
		this.visita=new HashMap<Airport, Airport>();
		visita.put(a1, null);
		
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				//recupero i due estremi
				Airport airport1= grafo.getEdgeSource(e.getEdge());
				Airport airport2= grafo.getEdgeTarget(e.getEdge());
				
				
				if(visita.containsKey(airport1) && !visita.containsKey(airport2)) {
					visita.put(airport2, airport1); //parent: a1
				}
				else if(visita.containsKey(airport2) && !visita.containsKey(airport1))
					visita.put(airport1,  airport2); //parent a2
				
				
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		//la visita del grafo
		while(it.hasNext()) {
			it.next();
		}
		//ottengo il percorso dall'albero di visita
		
		//se uno dei due aeroporti non è presente nell'albero di visita
		//   -> non c'è nessun percorso
		if(!visita.containsKey(a1) || !visita.containsKey(a2)) {
			return null;
		}
		
		//altrimenti, parto dal fondo e "risalgo" l'albero
		percorso.add(a2);
		Airport step=a2;
		while(visita.get(step) !=null) {
			//finchè non arriva alla radice, chi è il padre
			step=visita.get(step);
			percorso.add(step);
		}
		
		return percorso;
	}

}
