package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner scanner = new Scanner(System.in);
    private ConsumoAPI consumo = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=5957b2d8";

    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0) {
            var menu = """
                    \n1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por titulo
                    5 - Buscar série por ator
                    6 - Top 5 series
                    7 - Buscar série por categoria
                    8 - Buscar série por máximo de temporadas
                    9 - Buscar episódio por trecho
                    10 - Top 10 episódios por serie
                    11 - Buscar episodios a partir de uma data
                    0 - Sair
                    
                    opcao:\s""";

            System.out.print(menu);
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    busscarTop5Series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    top10EpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosAPartirData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
//        dadosSeries.add(dados);
        repositorio.save(new Serie(dados));
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.print("Digite o nome da série para busca: ");
        var nomeSerie = scanner.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.print("escolha uma serie do banco de dados: ");
        var escolha = scanner.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(escolha);

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream()
                            .map(e -> new Episodio(t.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        } else {
            System.out.println("serie nao encontrada");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();

        System.out.println("Series buscadas:");
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.print("escolha uma serie pelo nome: ");
        var escolha = scanner.nextLine();

        serieBuscada = repositorio.findByTituloContainingIgnoreCase(escolha);

        if (serieBuscada.isPresent()) {
            System.out.println("Dados da serie:\n" + serieBuscada.get());
        } else {
            System.out.println("serie nao encontrada");
        }

    }

    private void buscarSeriePorAtor() {
        System.out.print("escolha uma serie pelo ator: ");
        var nomeAtor = scanner.nextLine();
        System.out.print("filtre as serie a partir de uma avaliacao: ");
        Double avalicao = scanner.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avalicao);

        System.out.println("\nSeries em que " + nomeAtor + " trabalhou:");
        seriesEncontradas.forEach(s -> System.out.println("Titulo: " + s.getTitulo() + ", avaliação: " + s.getAvaliacao()));
    }

    private void busscarTop5Series() {
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(s -> System.out.println("Titulo: " + s.getTitulo() + ", avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriePorCategoria() {
        System.out.print("escolha uma serie pela categoria: ");
        var nomeCategoria = scanner.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeCategoria);
        List<Serie> seriesEncontradas = repositorio.findByGenero(categoria);

        System.out.println("Series no genero " + nomeCategoria);
        seriesEncontradas.forEach(s -> System.out.println("Titulo: " + s.getTitulo() + ", Genero: " + s.getGenero()));
    }

    private void filtrarSeriesPorTemporadaEAvaliacao() {
        System.out.print("digite o número máximo de temporadas que deseja: ");
        int maxTemp = scanner.nextInt();

        System.out.print("filtre as serie a partir de uma avaliacao: ");
        double avalicao = scanner.nextDouble();

        List<Serie> seriesEncontradas = repositorio.seriesPorTemporadaEAvaliacao(maxTemp, avalicao);
        System.out.println("Series encontradas: ");
        seriesEncontradas.forEach(s -> System.out.println("Titulo: " + s.getTitulo() + ", avaliação: " + s.getAvaliacao() + ", Temporadas: " + s.getTotalTemporadas()));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.print("escolha um episodio pelo nome: ");
        var nomeEpisodio = scanner.nextLine();

        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(nomeEpisodio);
        System.out.println("Episodios encontrados: ");
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumero(), e.getTitulo()));
    }

    private void top10EpisodiosPorSerie() {
        buscarSeriePorTitulo();

        if (serieBuscada.isPresent()) {
            List<Episodio> episodiosEncontrados = repositorio.top10Episodios(serieBuscada.get());
            System.out.println("Série: " + episodiosEncontrados.get(0).getSerie().getTitulo());
            episodiosEncontrados.forEach(e ->
                    System.out.printf("Avaliação %s - T%sE%s - %s\n", e.getAvaliacao(), e.getTemporada(),
                            e.getNumero(), e.getTitulo()));
        }
    }

    private void buscarEpisodiosAPartirData() {
        buscarSeriePorTitulo();

        if (serieBuscada.isPresent()) {
            System.out.print("data limite de lancamento: ");
            int anoLancamento = scanner.nextInt();
            scanner.nextLine();

            List<Episodio> episodiosEncontrados = repositorio.episodiosPorAnoESerie(serieBuscada.get(), anoLancamento);
            System.out.println("Série: " + episodiosEncontrados.get(0).getSerie().getTitulo());
            episodiosEncontrados.forEach(e ->
                    System.out.printf("%s T%sE%s - %s\n", e.getDataLancamento().getYear(), e.getTemporada(),
                            e.getNumero(), e.getTitulo()));
        }
    }

    public void adicionarEpisodiosParaTodasSeries() {
        List<Serie> series = repositorio.findAll();

        series.forEach(serie -> {
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serie.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serie.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(t -> t.episodios().stream()
                            .map(e -> new Episodio(t.numero(), e)))
                    .collect(Collectors.toList());

            serie.setEpisodios(episodios);
            repositorio.save(serie);
        });

        System.out.println("Episódios adicionados para todas as séries.");
    }
}