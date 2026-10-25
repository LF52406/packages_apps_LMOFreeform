# LMOFreeform

Based on [Mi-Freeform](https://github.com/sunshine0523/Mi-FreeForm/).

## License

```
Copyright (C) 2021-2023  sunshine0523
Copyright (C) 2024 The LibreMobileOS Foundation

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

## Android 17 / MistOS integration

This module implements virtual-display-backed floating **application windows**,
including resizing, window chrome, and edge-stashed mini-window gestures.

Related SystemUI control-center transient popup/media fixes are maintained in
[frameworks_base_qpr2](https://github.com/LF52406/frameworks_base_qpr2/commit/cb25db56e6ba74f0547a2ce51a7858cf09bf89bd).
That commit handles independent SystemUI media and popup overlays, **not**
LMOFreeform virtual display window geometry. It is **not** a source-level
build prerequisite for this module.
