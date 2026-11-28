"""Genera figuras 3D sintéticas (policubos) para el material de ejemplo de S1, S2 y S1b.

Por cada ítem KK (01..12) produce:
  KK-modelo.jpg            figura de referencia
  KK-alternativa-1.jpg     la misma figura girada (única correcta)
  KK-alternativa-2..5.jpg  la figura espejada y girada (distractores)

Todo es generado al azar con semilla fija: no proviene de ningún banco de ítems.
Uso: python3 generar_espacial.py <carpeta_salida>
"""

import itertools
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ANCHO, ALTO = 200, 214
FONDO = (0, 0, 0)
SEMILLA = 20261125
ITEMS = 12
CUBOS = 9
ESCALA = 24

DIRECCIONES = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]


def normalizar(celdas):
    mx = min(c[0] for c in celdas)
    my = min(c[1] for c in celdas)
    mz = min(c[2] for c in celdas)
    return frozenset((x - mx, y - my, z - mz) for x, y, z in celdas)


def rotaciones_cubo():
    """Las 24 rotaciones propias del cubo como matrices 3x3."""
    mats = []
    for perm in itertools.permutations(range(3)):
        for signos in itertools.product((1, -1), repeat=3):
            m = [[0] * 3 for _ in range(3)]
            for fila, col in enumerate(perm):
                m[fila][col] = signos[fila]
            det = (
                m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])
            )
            if det == 1:
                mats.append(m)
    return mats


ROTACIONES = rotaciones_cubo()


def aplicar(m, c):
    return tuple(sum(m[i][j] * c[j] for j in range(3)) for i in range(3))


def es_quiral(celdas):
    espejo = normalizar({(-x, y, z) for x, y, z in celdas})
    return all(normalizar({aplicar(m, c) for c in celdas}) != espejo for m in ROTACIONES)


def generar_policubo(rng):
    """Brazos rectos con giros en ángulo recto, al estilo de las figuras de rotación mental."""
    while True:
        celdas = [(0, 0, 0)]
        direccion_previa = None
        brazos = [rng.randint(2, 3) for _ in range(4)]
        for largo in brazos:
            opciones = [
                d
                for d in DIRECCIONES
                if direccion_previa is None
                or sum(a * b for a, b in zip(d, direccion_previa)) == 0
            ]
            d = rng.choice(opciones)
            for _ in range(largo):
                x, y, z = celdas[-1]
                celdas.append((x + d[0], y + d[1], z + d[2]))
            direccion_previa = d
        if len(set(celdas)) == len(celdas) and len(celdas) >= CUBOS and es_quiral(celdas):
            return normalizar(celdas)


def rotar(p, yaw, pitch, roll):
    x, y, z = p
    # roll (eje z)
    cr, sr = math.cos(roll), math.sin(roll)
    x, y = x * cr - y * sr, x * sr + y * cr
    # yaw (eje y)
    cy, sy = math.cos(yaw), math.sin(yaw)
    x, z = x * cy + z * sy, -x * sy + z * cy
    # pitch (eje x)
    cp, sp = math.cos(pitch), math.sin(pitch)
    y, z = y * cp - z * sp, y * sp + z * cp
    return x, y, z


CARAS = [
    ((1, 0, 0), [(1, 0, 0), (1, 1, 0), (1, 1, 1), (1, 0, 1)]),
    ((-1, 0, 0), [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)]),
    ((0, 1, 0), [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)]),
    ((0, -1, 0), [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)]),
    ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]),
    ((0, 0, -1), [(0, 0, 0), (0, 1, 0), (1, 1, 0), (1, 0, 0)]),
]
LUZ = (0.35, 0.75, 0.55)


def dibujar(celdas, yaw, pitch, roll):
    conjunto = set(celdas)
    cx = sum(c[0] for c in celdas) / len(celdas) + 0.5
    cy = sum(c[1] for c in celdas) / len(celdas) + 0.5
    cz = sum(c[2] for c in celdas) / len(celdas) + 0.5
    poligonos = []
    for c in celdas:
        for normal, vertices in CARAS:
            vecino = (c[0] + normal[0], c[1] + normal[1], c[2] + normal[2])
            if vecino in conjunto:
                continue
            n = rotar(normal, yaw, pitch, roll)
            if n[2] <= 0:
                continue
            puntos = [
                rotar((c[0] + v[0] - cx, c[1] + v[1] - cy, c[2] + v[2] - cz), yaw, pitch, roll)
                for v in vertices
            ]
            profundidad = sum(p[2] for p in puntos) / 4
            brillo = max(0.25, sum(a * b for a, b in zip(n, LUZ)))
            gris = int(120 + 125 * brillo)
            poligonos.append((profundidad, puntos, (gris, gris, gris + 4 if gris < 251 else 255)))
    poligonos.sort(key=lambda t: t[0])
    imagen = Image.new("RGB", (ANCHO, ALTO), FONDO)
    lapiz = ImageDraw.Draw(imagen)
    for _, puntos, color in poligonos:
        pantalla = [(ANCHO / 2 + p[0] * ESCALA, ALTO / 2 - p[1] * ESCALA) for p in puntos]
        lapiz.polygon(pantalla, fill=color, outline=(40, 40, 40))
    return imagen


def main():
    salida = Path(sys.argv[1] if len(sys.argv) > 1 else "bfa-espacial")
    salida.mkdir(parents=True, exist_ok=True)
    rng = random.Random(SEMILLA)
    for item in range(1, ITEMS + 1):
        figura = generar_policubo(rng)
        espejo = normalizar({(-x, y, z) for x, y, z in figura})
        pitch = math.radians(rng.uniform(20, 30))
        yaw_modelo = math.radians(rng.uniform(15, 50))
        kk = f"{item:02d}"
        dibujar(figura, yaw_modelo, pitch, 0).save(salida / f"{kk}-modelo.jpg", quality=85)
        giro = math.radians(rng.choice([70, 110, 130, 230, 250, 290]))
        dibujar(figura, yaw_modelo + giro, pitch, 0).save(
            salida / f"{kk}-alternativa-1.jpg", quality=85
        )
        for alt in range(2, 6):
            yaw = yaw_modelo + math.radians(rng.uniform(40, 320))
            dibujar(espejo, yaw, pitch, 0).save(
                salida / f"{kk}-alternativa-{alt}.jpg", quality=85
            )


if __name__ == "__main__":
    main()
