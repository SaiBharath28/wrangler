/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

grammar Directives;

options {
  language = Java;
}

@lexer::header {
/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
}

// =======================
// Parser Rules
// =======================

recipe
  : statements EOF
  ;

statements
  : ( COMMENT | macro | directive ';' | pragma ';' | ifStatement )*
  ;

directive
  : command
    ( codeblock
    | identifier
    | macro
    | text
    | number
    | bool
    | column
    | colList
    | numberList
    | boolList
    | stringList
    | numberRanges
    | properties
    )*?
  ;

ifStatement
  : ifStat elseIfStat* elseStat? '}'
  ;

ifStat
  : 'if' expression '{' statements
  ;

elseIfStat
  : '}' 'else' 'if' expression '{' statements
  ;

elseStat
  : '}' 'else' '{' statements
  ;

expression
  : '(' (~'(' | expression)* ')'
  ;

forStatement
  : 'for' '(' IDENTIFIER '=' expression ';' expression ';' expression ')' '{' statements '}'
  ;

macro
  : DOLLAR OBRACE (~OBRACE | macro | MACRO)*? CBRACE
  ;

pragma
  : '#pragma' (pragmaLoadDirective | pragmaVersion)
  ;

pragmaLoadDirective
  : 'load-directives' identifierList
  ;

pragmaVersion
  : 'version' NUMBER
  ;

codeblock
  : 'exp' SPACE* ':' condition
  ;

identifier
  : IDENTIFIER
  ;

properties
  : 'prop' ':' OBRACE (propertyList)+ CBRACE
  | 'prop' ':' OBRACE OBRACE (propertyList)+ CBRACE { notifyErrorListeners("Too many start paranthesis"); }
  | 'prop' ':' OBRACE (propertyList)+ CBRACE CBRACE { notifyErrorListeners("Too many start paranthesis"); }
  | 'prop' ':' (propertyList)+ CBRACE { notifyErrorListeners("Missing opening brace"); }
  | 'prop' ':' OBRACE (propertyList)+ { notifyErrorListeners("Missing closing brace"); }
  ;

propertyList
  : property (',' property)*
  ;

property
  : IDENTIFIER '=' ( text | number | bool | BYTE_SIZE | TIME_DURATION )
  ;

numberRanges
  : numberRange ( ',' numberRange)*
  ;

numberRange
  : NUMBER ':' NUMBER '=' value
  ;

value
  : number         #numericValue
  | text           #stringValue
  | identifier     #identifierValue
  | bool           #booleanValue
  | BYTE_SIZE      #byteSizeValue
  | TIME_DURATION  #timeDurationValue
  ;

ecommand
  : '!' IDENTIFIER
  ;

config
  : IDENTIFIER
  ;

column
  : COLUMN
  ;

text
  : STRING
  ;

number
  : NUMBER
  ;

bool
  : BOOL
  ;

condition
  : OBRACE (~CBRACE | condition)* CBRACE
  ;

command
  : IDENTIFIER
  ;

colList
  : COLUMN (',' COLUMN)+
  ;

numberList
  : NUMBER (',' NUMBER)+
  ;

boolList
  : BOOL (',' BOOL)+
  ;

stringList
  : STRING (',' STRING)+
  ;

identifierList
  : IDENTIFIER (',' IDENTIFIER)*
  ;

// =======================
// Lexer Rules (all uppercase)
// =======================

fragment BYTE_UNIT
  : [kKmMgGtTpPeE] [i]? [bB]
  | [bB]
  ;

BYTE_SIZE
  : [0-9]+ ('.' [0-9]+)? SPACE* BYTE_UNIT
  ;

TIME_DURATION
  : [0-9]+ ('.' [0-9]+)? SPACE* ('ns'|'us'|'ms'|'s'|'m'|'h'|'d')
  ;

OBRACE   : '{';
CBRACE   : '}';
SCOLON   : ';';
OR       : '||';
AND      : '&&';
EQUALS   : '==';
NEQUALS  : '!=';
GTEQUALS : '>=';
LTEQUALS : '<=';
MATCH    : '=~';
NOTMATCH : '!~';
QUESTIONCOLON : '?:';
STARTSWITH : '=^';
NOTSTARTSWITH : '!^';
ENDSWITH : '=$';
NOTENDSWITH : '!$';
PLUSEQUAL : '+=' ;
SUBEQUAL : '-=' ;
MULEQUAL : '*=' ;
DIVEQUAL : '/=' ;
PEREQUAL : '%=' ;
ANDEQUAL : '&=' ;
OREQUAL  : '|=' ;
XOREQUAL : '^=' ;
POW      : '^' ;
EXTERNAL : '!' ;
GT       : '>' ;
LT       : '<' ;
ADD      : '+' ;
SUBTRACT : '-' ;
MULTIPLY : '*' ;
DIVIDE   : '/' ;
MODULUS  : '%' ;
OBRACKET : '[' ;
CBRACKET : ']' ;
OPAREN   : '(' ;
CPAREN   : ')' ;
ASSIGN   : '=' ;
COMMA    : ',' ;
QMARK    : '?' ;
COLON    : ':' ;
DOT      : '.' ;
AT       : '@' ;
PIPE     : '|' ;
BACKSLASH: '\\';
DOLLAR   : '$' ;
TILDE    : '~' ;

BOOL
  : 'true'
  | 'false'
  ;

NUMBER
  : INT ('.' DIGIT*)?
  ;

IDENTIFIER
  : [a-zA-Z_\-] [a-zA-Z_0-9\-]* ;

MACRO
  : [a-zA-Z_] [a-zA-Z_0-9]* ;

COLUMN
  : ':' [a-zA-Z_\-] [:a-zA-Z_0-9\-]* ;

STRING
  : '\'' ( ESCAPESEQUENCE | ~('\'' ) )* '\''
  | '"'  ( ESCAPESEQUENCE | ~('"') )* '"'
  ;

fragment ESCAPESEQUENCE
  : '\\' ('b'|'t'|'n'|'f'|'r'|'"'|'\''|'\\')
  | UNICODEESCAPE
  | OCTALESCAPE
  ;

fragment OCTALESCAPE
  : '\\' ('0'..'3') ('0'..'7') ('0'..'7')
  | '\\' ('0'..'7') ('0'..'7')
  | '\\' ('0'..'7')
  ;

fragment UNICODEESCAPE
  : '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
  ;

fragment HEXDIGIT
  : [0-9a-fA-F]
  ;

fragment INT
  : '-'? [1-9] DIGIT* [L]*
  | '0'
  ;

fragment DIGIT
  : [0-9]
  ;

COMMENT
  : ('//' ~[\r\n]* | '/*' .*? '*/' | '--' ~[\r\n]* ) -> skip
  ;

SPACE
  : [ \t\r\n\u000C]+ -> skip
  ;