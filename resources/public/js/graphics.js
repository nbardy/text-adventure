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
Graphics.drawGrid = function(ctx, grid, cell_width, cell_height, old_grid, force) {
  var row_count  = grid.length;
  var col_count = grid[0].length;
  for (var row = 0; row < row_count; row++) {
    for (var col = 0; col < col_count; col++) {
      var val = grid[row][col]
      // Get color
      if(!old_grid || (val !== old_grid[row][col]) || force) {
        ctx.fillStyle = text_ad.render.map.core.trans_color(val);
        ctx.fillRect(col * cell_width,
            row * cell_height,
            cell_width,
            cell_height);
        ctx.fillStyle = "white";
      }
      
    }
  }

}
