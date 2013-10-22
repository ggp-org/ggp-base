// Game interface description.
{
  [BOARD_INTERFACE_JS]

  // ============= GAME SPECIFIC ====================

  getCellForMove: function(move) {
  	return "cell_" + move[1] + move[2];
  },

  getTargetsForMove: function(move) {
      var targets = [];
      targets.push(document.getElementById("cell_" + move[3] + move[4]));
      
      return targets;
  }
}