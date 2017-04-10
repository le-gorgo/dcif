%--------------------------------------------------------------------------
% File     : GRP123-1.005 : TPTP v5.0.0. Released v1.2.0.
% Domain   : Group Theory (Quasigroups)
% Problem  : (3,2,1) conjugate orthogonality
% Version  : [Sla93] axioms.
% English  : If ab=xy and a*b = x*y then a=x and b=y, where c*b=a iff ab=c.
%            Generate the multiplication table for the specified quasi-
%            group with 5 elements.

% Refs     : [FSB93] Fujita et al. (1993), Automatic Generation of Some Res
%          : [Sla93] Slaney (1993), Email to G. Sutcliffe
%          : [Zha94] Zhang (1994), Email to G. Sutcliffe
%          : [SFS95] Slaney et al. (1995), Automated Reasoning and Exhausti
% Source   : [Sla93]
% Names    : QG1 [Sla93]
%          : QG1 [FSB93]
%          : QG1 [SFS95]
%          : Bennett QG1 [TPTP]

% Status   : Satisfiable
% Rating   : 0.00 v5.0.0, 0.29 v4.1.0, 0.25 v4.0.1, 0.00 v3.4.0, 0.20 v3.3.0, 0.00 v3.2.0, 0.33 v3.1.0, 0.50 v2.5.0, 0.40 v2.4.0, 0.00 v2.2.1, 0.67 v2.2.0, 1.00 v2.1.0
% Syntax   : Number of clauses     :   32 (   1 non-Horn;  26 unit;  31 RR)
%            Number of atoms       :   52 (   0 equality)
%            Maximal clause size   :    7 (   2 average)
%            Number of predicates  :    3 (   0 propositional; 1-3 arity)
%            Number of functors    :    5 (   5 constant; 0-0 arity)
%            Number of variables   :   27 (   0 singleton)
%            Maximal term depth    :    1 (   1 average)
% SPC      : CNF_SAT_EPR

% Comments : [Sla93]'s axiomatization has been modified for this.
%          : Substitution axioms are not needed, as any positive equality
%            literals should resolve on negative ones directly.
%          : [Zha94] has pointed out that either one of qg1_1
%            or qg1_2 may be used, as each implies the other in this
%            scenario, with the help of cancellation. The dependence
%            cannot be proved, so both have been left in here.
%          : tptp2X: -f tptp -s5 GRP123-1.g
%--------------------------------------------------------------------------
cnf(element_1,axiom,
    ( group_element(e_1) )).

cnf(element_2,axiom,
    ( group_element(e_2) )).

cnf(element_3,axiom,
    ( group_element(e_3) )).

cnf(element_4,axiom,
    ( group_element(e_4) )).

cnf(element_5,axiom,
    ( group_element(e_5) )).

cnf(e_1_is_not_e_2,axiom,
    ( ~ equalish(e_1,e_2) )).

cnf(e_1_is_not_e_3,axiom,
    ( ~ equalish(e_1,e_3) )).

cnf(e_1_is_not_e_4,axiom,
    ( ~ equalish(e_1,e_4) )).

cnf(e_1_is_not_e_5,axiom,
    ( ~ equalish(e_1,e_5) )).

cnf(e_2_is_not_e_1,axiom,
    ( ~ equalish(e_2,e_1) )).

cnf(e_2_is_not_e_3,axiom,
    ( ~ equalish(e_2,e_3) )).

cnf(e_2_is_not_e_4,axiom,
    ( ~ equalish(e_2,e_4) )).

cnf(e_2_is_not_e_5,axiom,
    ( ~ equalish(e_2,e_5) )).

cnf(e_3_is_not_e_1,axiom,
    ( ~ equalish(e_3,e_1) )).

cnf(e_3_is_not_e_2,axiom,
    ( ~ equalish(e_3,e_2) )).

cnf(e_3_is_not_e_4,axiom,
    ( ~ equalish(e_3,e_4) )).

cnf(e_3_is_not_e_5,axiom,
    ( ~ equalish(e_3,e_5) )).

cnf(e_4_is_not_e_1,axiom,
    ( ~ equalish(e_4,e_1) )).

cnf(e_4_is_not_e_2,axiom,
    ( ~ equalish(e_4,e_2) )).

cnf(e_4_is_not_e_3,axiom,
    ( ~ equalish(e_4,e_3) )).

cnf(e_4_is_not_e_5,axiom,
    ( ~ equalish(e_4,e_5) )).

cnf(e_5_is_not_e_1,axiom,
    ( ~ equalish(e_5,e_1) )).

cnf(e_5_is_not_e_2,axiom,
    ( ~ equalish(e_5,e_2) )).

cnf(e_5_is_not_e_3,axiom,
    ( ~ equalish(e_5,e_3) )).

cnf(e_5_is_not_e_4,axiom,
    ( ~ equalish(e_5,e_4) )).

cnf(product_total_function1,axiom,
    ( ~ group_element(X)
    | ~ group_element(Y)
    | product(X,Y,e_1)
    | product(X,Y,e_2)
    | product(X,Y,e_3)
    | product(X,Y,e_4)
    | product(X,Y,e_5) )).

cnf(product_total_function2,axiom,
    ( ~ product(X,Y,W)
    | ~ product(X,Y,Z)
    | equalish(W,Z) )).

cnf(product_right_cancellation,axiom,
    ( ~ product(X,W,Y)
    | ~ product(X,Z,Y)
    | equalish(W,Z) )).

cnf(product_left_cancellation,axiom,
    ( ~ product(W,Y,X)
    | ~ product(Z,Y,X)
    | equalish(W,Z) )).

cnf(product_idempotence,axiom,
    ( product(X,X,X) )).

cnf(qg1_1,negated_conjecture,
    ( ~ product(X1,Y1,Z1)
    | ~ product(X2,Y2,Z1)
    | ~ product(Z2,Y1,X1)
    | ~ product(Z2,Y2,X2)
    | equalish(X1,X2) )).

cnf(qg1_2,negated_conjecture,
    ( ~ product(X1,Y1,Z1)
    | ~ product(X2,Y2,Z1)
    | ~ product(Z2,Y1,X1)
    | ~ product(Z2,Y2,X2)
    | equalish(Y1,Y2) )).

%--------------------------------------------------------------------------
