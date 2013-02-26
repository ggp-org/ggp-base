; An implementation of a sample random gamer in Clojure.
; -Sam Schreiber

; NOTE: the implicit 'this symbol is bound to the local class.

(ns gamer_namespace)

(defn SampleClojureGamer []
  (proxy [org.ggp.base.player.gamer.statemachine.StateMachineGamer] []
    (getInitialStateMachine []
      (new org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine))

    (stateMachineSelectMove [timeout]
      (.
        (. this (getStateMachine) [])
        (getRandomMove
          (. this (getCurrentState) [])
          (. this (getRole) [])
        )
      )
    )
    
    (stateMachineMetaGame [timeout] ())
    (stateMachineAbort [] ())    
    (stateMachineStop [] ())
    (getName [] ())    
  )
)