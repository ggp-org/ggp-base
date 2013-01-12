; An implementation of the legal gamer in Clojure.
; -Sam Schreiber

; NOTE: the implicit 'this symbol is bound to the local class.

(ns gamer_namespace)

(defn LegalGamer []
  (proxy [org.ggp.base.player.gamer.statemachine.StateMachineGamer] []
    (getName []
      "LegalGamer (Clojure)")

    (getInitialStateMachine []
      (new org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine))

    (stateMachineMetaGame [timeout]
      ())

    (stateMachineSelectMove [timeout]
      (first
        (.
          (. this (getStateMachine) [])
          (getLegalMoves
            (. this (getCurrentState) [])
            (. this (getRole) [])
	      )
        )
      )
    )
    
    (stateMachineStop []
      ())      
  )
)