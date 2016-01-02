package controllers.helpers

/**
 * Created by pc3 on 05.12.14.
 */

object Pagination {

  def paginationHeaders(productsTotal:Long, productsPerPage:Long ) = Seq(
    "Pagination-Results-Per-Page"     -> productsPerPage.toString,
    "Pagination-Results-Total"        -> productsTotal.toString,
    "Pagination-Results-Pages-Total"  -> Math.round( Math.ceil( productsTotal.toDouble / productsPerPage.toDouble ) ).toString
  )

}
