<html>
<head>
    <meta charset="utf-8"/>
    <link href="http://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css"/>
    <link href="http://cdn.datatables.net/1.10.15/css/jquery.dataTables.min.css" rel="stylesheet" type="text/css"/>
    <script src="http://code.jquery.com/jquery-1.12.4.js" type="text/javascript"></script>
    <script src="http://cdn.datatables.net/1.10.15/js/jquery.dataTables.min.js" type="text/javascript"></script>
    <!--script(type='text/javascript')--><!--  $(document).ready(function() {--><!--  $('table.data').DataTable();-->
    <!--  });-->
    <style>
    body {
        font-family: 'Roboto', sans-serif;
        font-size: 13px;
        color: #606163;
        margin: 200px 30px 0 30px;
    }

    p {
        font-size: 16px;
    }

    thead {
        display: table-header-group;
    }

    .content {
        page-break-after: always;
    }

    .section {
        page-break-after: always;
    }

    .section ul {
        font-size: 16px;
    }

    .section table {
        border-collapse: collapse;
        font-size: 13px;
    }

    .section th, td {
        padding: 10px;
        text-align: left;
    }

    .section th {
        border-bottom: 2px solid #606163;
    }

    .section td {
        border-bottom: 1px solid #606163;
    }

    h1 {
        font-size: 44px;
    }

    h2 {
        font-size: 32px;
    }

    h3 {
        font-size: 24px;
    }

    h4 {
        font-size: 18px;
    }

    h5 {
        font-size: 15px;
    }

    p {
        font-size: 13px;
    }

    #header {
        text-align: center;
        width: 100%;
        page-break-after: always;
    }

    #header img {
        margin-top: 30px;
    }

    #header table {
        margin: 100px auto 0;
        max-width: 400px;
        border: 1px solid #606163;
        padding: 10px;
    }

    #header th {
        text-align: right;
        padding: 5px 10px;
    }

    #header td {
        text-align: left;
        padding: 5px 10px;
    }

    #content tr.unusedAll {
        color: red;
    }

    #content tr.unused {
        color: orange;
    }

    .overall {
        font-size: 15px !important;
    }

    .overall th, .overall td {
        vertical-align: top;
        border-bottom: 2px solid !important;
    }

    .overall ol {
        padding-left: 15px;
    }
    </style>
</head>
<body>
<div id="header">
    <img src="https://image3.redbull.com/rbx00023/0001/1/210/403/logos/logos-homepage/brandlogos/jpg/red-bull-mediahouse-01.jpg"/>
    <h1>Vind - Report</h1>
    <h2>${prettyToday}</h2>
    <table>
        <tr>
            <th>Name:</th>
            <td>${applicationName}</td>
        </tr>
        <tr>
            <th>App:</th>
            <td>${applicationName}</td>
        </tr>
        <tr>
            <th>Workspace:</th>
            <td>N/A</td>
        </tr>
        <tr>
            <th>Module:</th>
            <td>N/A</td>
        </tr>
        <tr>
            <th>from:</th>
            <td>${prettyFrom}</td>
        </tr>
        <tr>
            <th>until:</th>
            <td>${prettyTo}</td>
        </tr>
    </table>
</div>
<div id="content">
    <div class="section"><h2>Overall</h2>
        <table class="overall">
            <tr>
                <th>Number of Requests</th>
                <td>${requests}</td>
            </tr>
            <tr>
                <th>Top Day</th>
                <td>${formattedTopDays?keys[0]!"NA"} (${formattedTopDays?values[0]!"NA"})</td>
            </tr>
            <tr>
                <th>Top Searchers</th>
                <td>
                    <ol>
                        <#list topUsers?keys as user>
                            <li>${user} (${topUsers[user]})</li>
                        </#list>
                    </ol>
                </td>
            </tr>
        </table>
    </div>
    <div class="section"><h2>Filters</h2>
        <p>This part shows which fields are set for filters in facets, suggestions and advanced and how they are used
            within the given timerange.
            The evaluation for each filter type is split in three parts:<i>Fields</i>, <i>Values</i> and <i>Tips</i>.
            In <i>Fields</i> we outline the current configuration together with the real field usage.
            In <i>Values</i> we show the top usages for the each field which has a standard deviation greater than 10 in
            usage.
            In <i>Tips</i> we give some hints how the search experience may be improved.</p></div>
    <div class="section"><h3>Facets</h3><h4>Facet Fields</h4>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Name</th>
                <th>Position</th>
                <th>#Usages</th>
                <th>#Usages as 1.</th>
                <th>#Usages as 2.</th>
                <th>#Usages as 3.</th>
                <th>#Usages all Logs</th>
                <th>#Values</th>
                <th>#stdV</th>
            </tr>
            </thead>
            <tbody>
                <#list topFacetFields?keys as field>
                    <tr>
                        <td>${field?counter}</td>
                        <td>${field}</td>
                        <td>N/A</td>
                        <td>${topFacetFields[field]}</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>N/A</td>
                    </tr>
                </#list>
            </tbody>
        </table>
        <h4>Facet Values</h4>
        <p>Only considers fields with a standard deviation greater than 10, only considers top 25 values of the first 3
            search steps.
        <h5>Top 10 values for<i>Content Type</i></h5>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Value</th>
                <th>#Usages in 1</th>
                <th>#Usages in 2</th>
                <th>#Usages in 3</th>
                <th>#Usages</th>
            </tr>
            </thead>
            <tbody>
                <tr>
                    <td>1</td>
                    <td>VIDEO</td>
                    <td>1740</td>
                    <td>8311</td>
                    <td>579</td>
                    <td>10630</td>
                </tr>
            </tbody>
        </table>

        </p>
        <h4>Facet Tips</h4>
        <p>We would suggest:</p>
        <ul>
            <li><u>Video Version</u> should be pushed down, we suggest position 5</li>
        </ul>
    </div>
    <div class="section"><h3>Suggestions</h3><h4>Suggestion Fields</h4>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Name</th>
                <th>Position</th>
                <th>#Usages</th>
                <th>#Usages as 1.</th>
                <th>#Usages as 2.</th>
                <th>#Usages as 3.</th>
                <th>#Usages all Logs</th>
                <th>#Values</th>
                <th>#stdV</th>
            </tr>
            </thead>
            <tbody>
                <#list topSuggestionFields?keys as field>
                    <tr>
                        <td>${field?counter}</td>
                        <td>${field}</td>
                        <td>N/A</td>
                        <td>${topSuggestionFields[field]}</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>N/A</td>
                        <td>-</td>
                        <td>N/A</td>
                    </tr>
                </#list>
            </tbody>
        </table>
        <h4>Suggestion Values</h4>
        <p>Only considers fields with a standard deviation greater than 10, only considers top 25 values of the first 3
            search steps.<h5>Top 10 values for<i>Content Model Structure</i></h5>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Value</th>
                <th>#Usages in 1</th>
                <th>#Usages in 2</th>
                <th>#Usages in 3</th>
                <th>#Usages</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>1</td>
                <td>- (Main Dummy) / Red Bull King of the Rock</td>
                <td>190</td>
                <td>0</td>
                <td>0</td>
                <td>190</td>
            </tr>

            </tbody>
        </table>
        </p><h4>Suggestions Tips</h4>
        <p>We would suggest:</p>
        <ul>
            <li><u>Leisure Time</u> is not in used and might be removed from index</li>
        </ul>
    </div>
    <div class="section"><h3>Advanced Search</h3><h4>Advanced Search Fields</h4>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Name</th>
                <th>Position</th>
                <th>#Usages</th>
                <th>#Usages as 1.</th>
                <th>#Usages as 2.</th>
                <th>#Usages as 3.</th>
                <th>#Usages all Logs</th>
                <th>#Values</th>
                <th>#stdV</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>1</td>
                <td>Content Model Structure</td>
                <td>28</td>
                <td>20932</td>
                <td>13395</td>
                <td>5898</td>
                <td>1215</td>
                <td>25242</td>
                <td>-</td>
                <td>35.32</td>
            </tr>

            </tbody>
        </table>
        <h4>Advanced Search Values</h4>
        <p>Only considers fields with a standard deviation greater than 10, only considers top 25 values of the first 3
            search steps.<h5>Top 10 values for<i>Content Model Structure</i></h5>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Value</th>
                <th>#Usages in 1</th>
                <th>#Usages in 2</th>
                <th>#Usages in 3</th>
                <th>#Usages</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>1</td>
                <td>Season 2</td>
                <td>340</td>
                <td>0</td>
                <td>0</td>
                <td>340</td>
            </tr>

            </tbody>
        </table>
        </p><h4>Advanced Search Tips</h4>
        <p>We would suggest:</p>
        <ul>
            <li><u>Watchers</u> is not in used and can maybe be removed from index</li>
        </ul>
    </div>
    <div class="section"><h2>Fulltext</h2>
        <h3>Fulltext Queries (Top 100)</h3>
        <p>${totalTopQueries} queries with ${topQueries?size} distinct search terms.</p>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Value</th>
                <th>#Usages as 1.</th>
                <th>#Usages as 2.</th>
                <th>#Usages as 3.</th>
                <th>#Usages</th>
                <th>#Users</th>
            </tr>
            </thead>
            <tbody>
                <#list topQueries?keys as query>
                <tr>
                    <td>${query?counter}</td>
                    <td>${query}</td>
                    <td>N/A</td>
                    <td>N/A</td>
                    <td>N/A</td>
                    <td>${topQueries[query]}</td>
                    <td>N/A</td>
                </tr>
                </#list>
            </tbody>
        </table>
        <h3>Filtered Fulltext Queries (Top 100)</h3>
        <p>Filter: without MI*, SI*, BU*, AA* and number-based IDs</p>
        <p>14769 queries with 3133 distinct search terms.</p>
        <table class="data">
            <thead>
            <tr>
                <th>Ranking</th>
                <th>Value</th>
                <th>#Usages as 1.</th>
                <th>#Usages as 2.</th>
                <th>#Usages as 3.</th>
                <th>#Usages</th>
                <th>#Users</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>1</td>
                <td title="recap">recap</td>
                <td>0</td>
                <td>21</td>
                <td>6</td>
                <td>30</td>
                <td>10</td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>