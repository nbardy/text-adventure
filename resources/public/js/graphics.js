/**
 * @type {Object}
 * @const
 */
var Graphics = {};

/**
 * @param {Object} ctx
 * @param {string} text
 * @param {number} x
 * @param {number} y
 */
Graphics.drawGrid = function(ctx, grid, cell_width, cell_height) {
  var total  = grid.count;
  var o_total = grid[0].count;
  for (var i = 0; i < total; i++) {
    for (var j = 0; j < o_total; j++) {
      var val = grid[i,j];
      // Get color
       
      ctx.fillStyle = text_ad.graphics.trans_color([val,0,0]
      ctx.fillRect(i * cell_width,
                   j * cell_height,
                   cell_width,
                   cell_height);
      ctx.fillStyle = "white";
      ctx.fillText(i + ", " + j, i * cell_width, j * cell_height);
      
    }
  }

}
