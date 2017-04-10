%--------------------------------------------------------------------------
% File     : RNG004-3 : TPTP v6.3.0. Released v1.0.0.
% Domain   : Ring Theory
% Problem  : X*Y = -X*-Y
% Version  : [Wos65] axioms : Reduced & Augmented > Incomplete.
% English  :

% Refs     : [Wos65] Wos (1965), Unpublished Note
%          : [WM76]  Wilson & Minker (1976), Resolution, Refinements, and S
% Source   : [SPRFN]
% Names    : Problem 22 [Wos65]
%          : wos22 [WM76]

% Status   : Unsatisfiable
% Rating   : 0.25 v6.2.0, 0.33 v6.1.0, 0.50 v6.0.0, 0.44 v5.5.0, 0.50 v5.4.0, 0.61 v5.3.0, 0.50 v5.2.0, 0.46 v5.1.0, 0.50 v5.0.0, 0.53 v4.1.0, 0.60 v4.0.1, 0.29 v3.7.0, 0.43 v3.4.0, 0.60 v3.3.0, 0.33 v3.2.0, 0.67 v3.1.0, 0.33 v2.7.0, 0.38 v2.6.0, 0.57 v2.4.0, 0.57 v2.3.0, 0.43 v2.2.1, 0.67 v2.1.0, 0.71 v2.0.0
% Syntax   : Number of clauses     :   34 (   0 non-Horn;  12 unit;  23 RR)
%            Number of atoms       :   85 (   0 equality)
%            Maximal clause size   :    5 (   2 average)
%            Number of predicates  :    3 (   0 propositional; 2-3 arity)
%            Number of functors    :    8 (   5 constant; 0-2 arity)
%            Number of variables   :  111 (   2 singleton)
%            Maximal term depth    :    2 (   1 average)
% SPC      : CNF_UNS_RFO_NEQ_HRN

% Comments :
%--------------------------------------------------------------------------
%----Include ring theory axioms
%include('Axioms/RNG001-0.ax').
%--------------------------------------------------------------------------
cnf(reflexivity,axiom,
    ( equalish(X,X) )).

cnf(symmetry,axiom,
    ( ~ equalish(X,Y)
    | equalish(Y,X) )).

cnf(transitivity,axiom,
    ( ~ equalish(X,Y)
    | ~ equalish(Y,Z)
    | equalish(X,Z) )).

%----Equality axioms for additive operator
cnf(additive_inverse_substitution,axiom,
    ( ~ equalish(X,Y)
    | equalish(additive_inverse(X),additive_inverse(Y)) )).

cnf(add_substitution1,axiom,
    ( ~ equalish(X,Y)
    | equalish(add(X,W),add(Y,W)) )).

%----This axiom omited in this version
%input_clause(add_substitution2,axiom,
%    [--equalish(X,Y),
%     ++equalish(add(W,X),add(W,Y))]).

cnf(sum_substitution1,axiom,
    ( ~ equalish(X,Y)
    | ~ sum(X,W,Z)
    | sum(Y,W,Z) )).

cnf(sum_substitution2,axiom,
    ( ~ equalish(X,Y)
    | ~ sum(W,X,Z)
    | sum(W,Y,Z) )).

cnf(sum_substitution3,axiom,
    ( ~ equalish(X,Y)
    | ~ sum(W,Z,X)
    | sum(W,Z,Y) )).

%----Equality axioms for multiplicative operator
cnf(multiply_substitution1,axiom,
    ( ~ equalish(X,Y)
    | equalish(multiply(X,W),multiply(Y,W)) )).

%----This axiom omited in this version
%input_clause(multiply_substitution2,axiom,
%    [--equalish(X,Y),
%     ++equalish(multiply(W,X),multiply(W,Y))]).

cnf(product_substitution1,axiom,
    ( ~ equalish(X,Y)
    | ~ product(X,W,Z)
    | product(Y,W,Z) )).

cnf(product_substitution2,axiom,
    ( ~ equalish(X,Y)
    | ~ product(W,X,Z)
    | product(W,Y,Z) )).

cnf(product_substitution3,axiom,
    ( ~ equalish(X,Y)
    | ~ product(W,Z,X)
    | product(W,Z,Y) )).

cnf(additive_identity1,axiom,
    ( sum(additive_identity,X,X) )).

cnf(additive_identity2,axiom,
    ( sum(X,additive_identity,X) )).

cnf(closure_of_multiplication,axiom,
    ( product(X,Y,multiply(X,Y)) )).

cnf(closure_of_addition,axiom,
    ( sum(X,Y,add(X,Y)) )).

cnf(left_inverse,axiom,
    ( sum(additive_inverse(X),X,additive_identity) )).

cnf(right_inverse,axiom,
    ( sum(X,additive_inverse(X),additive_identity) )).

cnf(associativity_of_addition1,axiom,
    ( ~ sum(X,Y,U)
    | ~ sum(Y,Z,V)
    | ~ sum(U,Z,W)
    | sum(X,V,W) )).

cnf(associativity_of_addition2,axiom,
    ( ~ sum(X,Y,U)
    | ~ sum(Y,Z,V)
    | ~ sum(X,V,W)
    | sum(U,Z,W) )).

cnf(commutativity_of_addition,axiom,
    ( ~ sum(X,Y,Z)
    | sum(Y,X,Z) )).

cnf(associativity_of_multiplication1,axiom,
    ( ~ product(X,Y,U)
    | ~ product(Y,Z,V)
    | ~ product(U,Z,W)
    | product(X,V,W) )).

cnf(associativity_of_multiplication2,axiom,
    ( ~ product(X,Y,U)
    | ~ product(Y,Z,V)
    | ~ product(X,V,W)
    | product(U,Z,W) )).

cnf(distributivity1,axiom,
    ( ~ product(X,Y,V1)
    | ~ product(X,Z,V2)
    | ~ sum(Y,Z,V3)
    | ~ product(X,V3,V4)
    | sum(V1,V2,V4) )).

cnf(distributivity2,axiom,
    ( ~ product(X,Y,V1)
    | ~ product(X,Z,V2)
    | ~ sum(Y,Z,V3)
    | ~ sum(V1,V2,V4)
    | product(X,V3,V4) )).

cnf(distributivity3,axiom,
    ( ~ product(Y,X,V1)
    | ~ product(Z,X,V2)
    | ~ sum(Y,Z,V3)
    | ~ product(V3,X,V4)
    | sum(V1,V2,V4) )).

cnf(distributivity4,axiom,
    ( ~ product(Y,X,V1)
    | ~ product(Z,X,V2)
    | ~ sum(Y,Z,V3)
    | ~ sum(V1,V2,V4)
    | product(V3,X,V4) )).

%-----Equality axioms for operators
cnf(addition_is_well_defined,axiom,
    ( ~ sum(X,Y,U)
    | ~ sum(X,Y,V)
    | equalish(U,V) )).

cnf(multiplication_is_well_defined,axiom,
    ( ~ product(X,Y,U)
    | ~ product(X,Y,V)
    | equalish(U,V) )).

cnf(multiplicative_identity1,axiom,
    ( product(additive_identity,X,additive_identity) )).

cnf(multiplicative_identity2,axiom,
    ( product(X,additive_identity,additive_identity) )).

cnf(a_times_b,hypothesis,
    ( product(a,b,c) )).

cnf(a_inverse_times_b_inverse,hypothesis,
    ( product(additive_inverse(a),additive_inverse(b),d) )).

cnf(prove_c_equals_d,negated_conjecture,
    ( ~ equalish(c,d) )).

%--------------------------------------------------------------------------
