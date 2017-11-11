package krangl



/** Create a data-frame from a list of objects */
fun <T> List<T>.asDataFrame(mapping: (T) -> DataFrameRow) = DataFrame.fromRecords(this, mapping)

/** Create a data-frame from a list of objects */
fun <T> DataFrame.Companion.fromRecords(records: List<T>, mapping: (T) -> DataFrameRow): DataFrame {
    val rowData = records.map { mapping(it) }
    val columnNames = mapping(records.first()).keys

    val columnData = columnNames.map { it to emptyList<Any?>().toMutableList() }.toMap()

    for (record in rowData) {
        columnData.forEach { colName, colData -> colData.add(record[colName]) }
    }

    return columnData.map { (name, data) -> handleListErasure(name, data) }.asDataFrame()
}


/**
Create a new data frame in place.

@sample krangl.samples.DokkaSamplesKt.buildDataFrameof
 */
fun dataFrameOf(vararg header: String) = InplaceDataFrameBuilder(header.toList())

// added to give consistent api entrypoint

fun DataFrame.Companion.of(vararg header: String) = krangl.dataFrameOf(*header)

internal fun SimpleDataFrame.addColumn(dataCol: DataCol): SimpleDataFrame =
        SimpleDataFrame(cols.toMutableList().apply { add(dataCol) })



class InplaceDataFrameBuilder(private val header: List<String>) {


    operator fun invoke(vararg tblData: Any?): DataFrame {
        //        if(tblData.first() is Iterable<Any?>) {
        //            tblData = tblData.first() as Iterable<Any?>
        //        }


        // 1) break into columns
        val rawColumns: List<List<Any?>> = tblData.toList()
                .mapIndexed { i, any -> i.rem(header.size) to any }
                .groupBy { it.first }.values.map {
            it.map { it.second }
        }


        // 2) infer column type by peeking into column data
        val tableColumns = header.zip(rawColumns).map {
            handleListErasure(it.first, it.second)
        }

        require(tableColumns.map { it.length }.distinct().size == 1) {
            "Provided data does not coerce to tablular shape"
        }

        // 3) bind into data-frame
        return SimpleDataFrame(tableColumns)
    }


    //    operator fun invoke(values: List<Any?>): DataFrame {
    //        return invoke(values.toTypedArray())
    //    }

}
