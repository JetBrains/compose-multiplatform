import matplotlib.path as mpath
import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
import matplotlib.ticker as plticker

Path = mpath.Path

def draw_curve(ax, points, color, label):
    ax.add_patch(quadratic_curve(points, color, label))
    ax.add_patch(app_bar_line(points[0]))
    # Add marker showing where the curve intercept is
    ax.text(points[2][0], points[2][1], '{}, {}'.format(points[2][0], points[2][1]))

def quadratic_curve(points, color, label):
    return mpatches.PathPatch(
        Path(points, [Path.MOVETO, Path.CURVE3, Path.CURVE3]),
        fc='none',
        ec=color,
        transform=ax.transData,
        linewidth=3,
        label=label)

def app_bar_line(curve_start):
    line_start = (-9, curve_start[1])
    return mpatches.PathPatch(
        Path([line_start, curve_start], [Path.MOVETO, Path.LINETO]),
        fc='none',
        ec='black',
        transform=ax.transData,
        linewidth=3,
        linestyle=':')

fig, ax = plt.subplots()
fig.set_figheight(20)
fig.set_figwidth(20)
ax.set_aspect('equal')
ax.set_xlim([-9, 6])
ax.set_ylim([-6, 7])

# Set axes interval to be every 1
locator = plticker.MultipleLocator(base=1.0)
ax.xaxis.set_major_locator(locator)
ax.yaxis.set_major_locator(locator)

# Draw circle representing the cutout
ax.add_artist(plt.Circle((0, 0), 5, color='grey', fill=False, linewidth=3, linestyle='--'))
# Draw dot for circle origin
ax.plot([0], [0], 'ok')

# Points correspond to quadratic bezier curve points - curve start, control point, end point
CENTER_CURVE_POINTS = [(-8, 0), (-6, 0), (-4.16, -2.76)]
ABOVE_CURVE_OUTSIDE_RADIUS_POINTS = [(-7.33, 2.5), (-5.33, 2.5), (-4.96, -0.59)]
ABVE_CURVE_INSIDE_RADIUS_POINTS = [(-5.18, 4.5), (-3.18, 4.5), (-4.33, 2.49)]
BELOW_CURVE_POINTS = [(-7.33, -2.5), (-5.33, -2.5), (-2.72, -4.19)]

draw_curve(ax, ABVE_CURVE_INSIDE_RADIUS_POINTS, 'orange', 'Above cutout, within cutout radius')
draw_curve(ax, ABOVE_CURVE_OUTSIDE_RADIUS_POINTS, 'blue', 'Above cutout, outside cutout radius')
draw_curve(ax, CENTER_CURVE_POINTS, 'red', 'Vertically aligned with cutout')
draw_curve(ax, BELOW_CURVE_POINTS, 'green', 'Below cutout')

# Show graph with legend using the labels
ax.legend()
plt.show()
