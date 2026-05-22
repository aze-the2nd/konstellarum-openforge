# -*- mode: python ; coding: utf-8 -*-


a = Analysis(
    ['taskclock.py'],
    pathex=[],
    binaries=[('/opt/data/.local/share/uv/python/cpython-3.12.13-linux-x86_64-gnu/lib/libtcl9.0.so', '.'), ('/opt/data/.local/share/uv/python/cpython-3.12.13-linux-x86_64-gnu/lib/libtcl9tk9.0.so', '.')],
    datas=[('/opt/data/.local/share/uv/python/cpython-3.12.13-linux-x86_64-gnu/lib/tcl9.0', 'tcl9.0'), ('/opt/data/.local/share/uv/python/cpython-3.12.13-linux-x86_64-gnu/lib/tcl9', 'tcl9'), ('/opt/data/.local/share/uv/python/cpython-3.12.13-linux-x86_64-gnu/lib/tk9.0', 'tk9.0')],
    hiddenimports=[],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
)
pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name='TaskClock',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
