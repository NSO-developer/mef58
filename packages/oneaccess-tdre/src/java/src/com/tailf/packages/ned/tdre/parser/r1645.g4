grammar r1645;

@header {
package com.tailf.packages.ned.tdre.parser;
}


data : ('GET'|'SET') '{' (select)* '}';

select : 'SELECT' NAME '{' (select | select_list | select_list_name)* '}';
select_list : 'SELECT' NAME '{' (liste|select)+ '}';
select_list_name : 'SELECT' ENTRY '{' liste '}';
liste : 'LIST' '{' (attr | attr_list)* '}';

attr_list : NAME '=' '{' (index | attr | attr_list)* '}';
// Can consist only of (index+) | (attr | attr_list)*

index : INDEX '=' '{' (attr | attr_list)* '}';
attr : NAME '=' (NAME | VALUE | STR);

NAME : [a-zA-Z0-9_][a-zA-Z0-9_-]*;
VALUE : [0-9]+;
STR : [a-zA-Z0-9.,:;_<>/-]+ | '"'[a-zA-Z0-9%.,:;_<>/ -]*'"';
ENTRY : [a-zA-Z_][a-zA-Z_0-9]+'['[a-zA-Z_][a-zA-Z0-9_-]+']';
INDEX : '['[a-zA-Z0-9]+']';
WS: [ \t\n\r]+ -> skip ;
