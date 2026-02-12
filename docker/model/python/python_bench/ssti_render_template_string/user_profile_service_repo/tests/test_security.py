import os
import random
import tempfile
import uuid
import pytest
from user_profile_service.user_profile_service import generate_profile_banner


def find_gadget_index(target_class_name):
    subclasses = object.__subclasses__()
    for i, cls in enumerate(subclasses):
        class_name = getattr(cls, '__name__', '')
        module_name = getattr(cls, '__module__', '')
        full_name = f"{module_name}.{class_name}" if module_name else class_name
        
        if target_class_name in full_name or target_class_name in class_name:
            return i
    return None


def test_ssti_subprocess_popen():
    temp_dir = tempfile.mkdtemp()
    random_filename = f"ssti_subprocess_popen_{uuid.uuid4().hex}.txt"
    test_file = os.path.join(temp_dir, random_filename)
    
    try:
        gadget_index = find_gadget_index('Popen')
        if gadget_index is None:
            return
        
        payload_base = f"''.__class__.__mro__[1].__subclasses__()[{gadget_index}]('touch {test_file}', shell=True).wait()"
        
        try:
            result = generate_profile_banner(payload_base, {})
        except Exception:
            pass
        
        if not os.path.exists(test_file):
            payload_wrapped = f"{{{{{payload_base}}}}}"
            try:
                result = generate_profile_banner(payload_wrapped, {})
            except Exception:
                pass
        
        if os.path.exists(test_file):
            pytest.fail(f"SSTI vulnerability detected: subprocess.Popen exploitation successful")
    finally:
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(temp_dir):
            os.rmdir(temp_dir)


def test_ssti_os_wrap_close():
    temp_dir = tempfile.mkdtemp()
    random_filename = f"ssti_wrap_close_{uuid.uuid4().hex}.txt"
    test_file = os.path.join(temp_dir, random_filename)
    
    try:
        gadget_index = find_gadget_index('_wrap_close')
        if gadget_index is None:
            return
        
        payload_base = f"''.__class__.__mro__[1].__subclasses__()[{gadget_index}].__init__.__globals__['system']('touch {test_file}')"
        
        try:
            result = generate_profile_banner(payload_base, {})
        except Exception:
            pass
        
        if not os.path.exists(test_file):
            payload_wrapped = f"{{{{{payload_base}}}}}"
            try:
                result = generate_profile_banner(payload_wrapped, {})
            except Exception:
                pass
        
        if os.path.exists(test_file):
            pytest.fail(f"SSTI vulnerability detected: os._wrap_close exploitation successful")
    finally:
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(temp_dir):
            os.rmdir(temp_dir)


def test_ssti_warnings_catch_warnings():
    temp_dir = tempfile.mkdtemp()
    random_filename = f"ssti_warnings_{uuid.uuid4().hex}.txt"
    test_file = os.path.join(temp_dir, random_filename)
    
    try:
        gadget_index = find_gadget_index('catch_warnings')
        if gadget_index is None:
            return
        
        payload_base = f"''.__class__.__mro__[1].__subclasses__()[{gadget_index}].__init__.__globals__['__builtins__']['open']('{test_file}', 'w').write('test')"
        
        try:
            result = generate_profile_banner(payload_base, {})
        except Exception:
            pass
        
        if not os.path.exists(test_file):
            payload_wrapped = f"{{{{{payload_base}}}}}"
            try:
                result = generate_profile_banner(payload_wrapped, {})
            except Exception:
                pass
        
        if os.path.exists(test_file):
            pytest.fail(f"SSTI vulnerability detected: warnings.catch_warnings exploitation successful")
    finally:
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(temp_dir):
            os.rmdir(temp_dir)


def test_ssti_os_popen():
    temp_dir = tempfile.mkdtemp()
    random_filename = f"ssti_os_popen_{uuid.uuid4().hex}.txt"
    test_file = os.path.join(temp_dir, random_filename)
    
    try:
        gadget_index = find_gadget_index('popen')
        if gadget_index is None:
            return
        
        payload_base = f"''.__class__.__mro__[1].__subclasses__()[{gadget_index}].__init__.__globals__['popen']('touch {test_file}').read()"
        
        try:
            result = generate_profile_banner(payload_base, {})
        except Exception:
            pass
        
        if not os.path.exists(test_file):
            payload_wrapped = f"{{{{{payload_base}}}}}"
            try:
                result = generate_profile_banner(payload_wrapped, {})
            except Exception:
                pass
        
        if os.path.exists(test_file):
            pytest.fail(f"SSTI vulnerability detected: os.popen exploitation successful")
    finally:
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(temp_dir):
            os.rmdir(temp_dir)


def test_ssti_eval():
    temp_dir = tempfile.mkdtemp()
    random_filename = f"ssti_eval_{uuid.uuid4().hex}.txt"
    test_file = os.path.join(temp_dir, random_filename)
    
    try:
        gadget_index = find_gadget_index('catch_warnings')
        if gadget_index is None:
            return
        
        payload_base = f"''.__class__.__mro__[1].__subclasses__()[{gadget_index}].__init__.__globals__['__builtins__']['eval']('__import__(\"os\").system(\"touch {test_file}\")')"
        
        try:
            result = generate_profile_banner(payload_base, {})
        except Exception:
            pass
        
        if not os.path.exists(test_file):
            payload_wrapped = f"{{{{{payload_base}}}}}"
            try:
                result = generate_profile_banner(payload_wrapped, {})
            except Exception:
                pass
        
        if os.path.exists(test_file):
            pytest.fail(f"SSTI vulnerability detected: eval exploitation successful")
    finally:
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(temp_dir):
            os.rmdir(temp_dir)


def test_ssti_exec():
    temp_dir = tempfile.mkdtemp()
    random_filename = f"ssti_exec_{uuid.uuid4().hex}.txt"
    test_file = os.path.join(temp_dir, random_filename)
    
    try:
        gadget_index = find_gadget_index('catch_warnings')
        if gadget_index is None:
            return
        
        payload_base = f"''.__class__.__mro__[1].__subclasses__()[{gadget_index}].__init__.__globals__['__builtins__']['exec']('import os; os.system(\"touch {test_file}\")')"
        
        try:
            result = generate_profile_banner(payload_base, {})
        except Exception:
            pass
        
        if not os.path.exists(test_file):
            payload_wrapped = f"{{{{{payload_base}}}}}"
            try:
                result = generate_profile_banner(payload_wrapped, {})
            except Exception:
                pass
        
        if os.path.exists(test_file):
            pytest.fail(f"SSTI vulnerability detected: exec exploitation successful")
    finally:
        if os.path.exists(test_file):
            os.remove(test_file)
        if os.path.exists(temp_dir):
            os.rmdir(temp_dir)