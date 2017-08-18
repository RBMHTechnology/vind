package co.redlink.demo.step5;

import co.redlink.demo.step5.guardian.ResultTransformer;
import co.redlink.demo.step5.service.SearchService;
import org.apache.commons.lang.StringUtils;

import static spark.Spark.get;

public class SearchApplication {

	public static void main(String[] args) {

		try (SearchService search = new SearchService(args[0])) {

			get("/index", (req, res) -> search.index());

			get("/search", (req, res) -> search.search(req.queryParams("q"), req.queryParamsValues("filter")), new ResultTransformer());

			get("/news", (req, res) -> search.search(
							req.queryParams("q"),
							Integer.parseInt(StringUtils.defaultIfBlank(req.queryParams("p"), "1")),
							SearchService.Sort.pares(req.queryParams("sort"))
					),
					new ResultTransformer()
			);

			get("/suggest", (req, res) -> search.suggest(req.queryParams("q")), new ResultTransformer());
		}

	}



}
