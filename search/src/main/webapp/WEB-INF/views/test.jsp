<%--
  Created by IntelliJ IDEA.
  User: Administrator
  Date: 13-5-17
  Time: 下午1:45
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title></title>
    <script>
        function search(){
            var f = document.getElementById("ff")
            var s = document.getElementById("tt").value
            f.action = "search/product/xml/ru/*/"+s+"/1/100/f+"
        }
    </script>
</head>
<body>
<form id="ff" name="ff" method="post" action="search/product/xml">
    <input type="text" id="keyword" name="keyword">
    <input type="test" id="country" name="country" value="ru">
    <input type="submit" value="搜索">
</form>
<br>
<form method="post" action="search/product/test/xml">
    <input type="text" id="search" name="search">

    <input type="submit" value="搜索">
</form>
</body>
</html>