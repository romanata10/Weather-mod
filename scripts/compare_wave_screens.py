#!/usr/bin/env python3
"""Report moving-frame evidence only; lighting/particles are confounders."""
from PIL import Image, ImageChops, ImageStat

with Image.open('wave-screen.png') as before, Image.open('wave-later-screen.png') as after:
    if before.size != after.size:
        raise SystemExit('Wave framebuffer dimensions changed')
    w,h=before.size
    # Crop the ocean surface and exclude the top-right tutorial toast.
    region=(int(w*.20),int(h*.34),int(w*.58),int(h*.82))
    delta=ImageChops.difference(before.convert('RGB').crop(region),
                                after.convert('RGB').crop(region))
    count=sum(max(rgb)>32 for rgb in delta.getdata())
    total=delta.width*delta.height
    mean=sum(ImageStat.Stat(delta).mean)/3
    print(f'::notice::Ocean frame pair 1 second apart: {count}/{total} changed '
          f'pixels ({100*count/total:.1f}%), mean RGB delta {mean:.1f}; '
          'moving rain/clouds may contribute; NOT proof of a moving wavefront.')
