<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="head" fragment="true" required="false" %>
<%@ attribute name="title" required="false" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">

    <title>${not empty title ? title : "Semantics Authentication"}</title>

    <!-- Bootstrap 5.3.3 from WebJars -->
    <link rel="stylesheet" href="/webjars/bootstrap/5.3.3/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
    <script src="/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"></script>

    <style>
        table.pretty-table {
            border-collapse: separate;
            border-spacing: 0;
            margin: 0 !important;
        }

        .pretty-table > tbody > tr:first-of-type > td {
            border-top: 0;
        }

        .pretty-table > thead > tr > th {
            background-clip: border-box;
            border-color: rgba(0, 0, 0, 0.125) !important;
            position: sticky !important;
            top: 0 !important;
            z-index: 1;
        }

        html, body {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            background-color: #efebeb;
        }

        #loading {
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            background: rgba(0, 0, 0, 0.3);
            z-index: 1050;
            display: none;
        }

        #loading.active {
            display: flex;
            align-items: center;
            justify-content: center;
        }
    </style>

    <jsp:invoke fragment="head"/>
</head>

<body>
    <div class="container-fluid p-0 m-0" style="height: 100vh; width: 100vw;">
        <jsp:doBody/>
    </div>

    <div id="loading">
        <i class="fas fa-circle-notch fa-spin fa-5x text-info"></i>
    </div>
</body>
</html>
